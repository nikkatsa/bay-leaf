package com.nikoskatsanos.bayleaf.netty;

import com.nikoskatsanos.bayleaf.core.ConnectorRegistry;
import com.nikoskatsanos.bayleaf.core.auth.Authenticator;
import com.nikoskatsanos.bayleaf.core.auth.Authorizer;
import com.nikoskatsanos.bayleaf.netty.dispatch.DispatchingStrategy;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.util.Objects;
import javax.net.ssl.SSLException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class BayLeafServer implements AutoCloseable {

    private final int port;
    private final BayLeafServerChannelInitializer channelInitializer;
    private EventLoopGroup eventLoopGroup;
    private EventLoopGroup workerGroup;

    public void start() {
        logger.info("Starting {}", this.getClass().getSimpleName());

        this.eventLoopGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        try {
            final ServerBootstrap server = new ServerBootstrap();
            server.option(ChannelOption.SO_BACKLOG, 1024);
            server.group(this.eventLoopGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(NettyProps.BAY_LEAF_NETTY_LOG_LEVEL))
                .childHandler(this.channelInitializer)
                .validate();
            final ChannelFuture channelFuture = server.bind(this.port);
            channelFuture.channel().closeFuture().sync();
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void close() throws Exception {
        logger.info("Closing {}", this.getClass().getSimpleName());
        if (Objects.nonNull(this.workerGroup)) {
            this.workerGroup.shutdownGracefully();
        }
        if (Objects.nonNull(this.eventLoopGroup)) {
            this.eventLoopGroup.shutdownGracefully();
        }
    }

    public static class Builder {

        private int port;

        private Authenticator authenticator;
        private Authorizer authorizer;

        private ConnectorRegistry connectorRegistry;

        private SslContext sslContext;

        private DispatchingStrategy dispatchingStrategy = DispatchingStrategy.defaultStrategy();

        public Builder withPort(final int port) {
            this.port = port;
            return this;
        }

        public Builder withAuthenticator(final Authenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        public Builder withAuthorizer(final Authorizer authorizer) {
            this.authorizer = authorizer;
            return this;
        }

        public Builder withConnectorRegistry(final ConnectorRegistry connectorRegistry) {
            this.connectorRegistry = connectorRegistry;
            return this;
        }

        public Builder withSSLContext(final File cert, final File privateKey, final String privateKeyPass) {
            try {
                this.sslContext = SslContextBuilder.forServer(cert, privateKey, privateKeyPass).build();
                return this;
            } catch (final SSLException e) {
                throw new RuntimeException(String.format("Failed to create SSL context for Cert=%s, Private=%s", cert.getAbsolutePath(), privateKey.getAbsolutePath()));
            }
        }

        public Builder withDispatchingStrategy(final DispatchingStrategy dispatchingStrategy) {
            this.dispatchingStrategy = dispatchingStrategy;
            return this;
        }

        public BayLeafServer build() {
            final BayLeafServerSessionHandler sessionHandler = new BayLeafServerSessionHandler(this.authenticator, this.authorizer);
            final BayLeafServerServiceMessageHandler serviceMessageHandler = new BayLeafServerServiceMessageHandler(this.connectorRegistry, this.dispatchingStrategy);
            final BayLeafServerChannelInitializer channelInitializer = new BayLeafServerChannelInitializer(sessionHandler, serviceMessageHandler, this.sslContext);
            return new BayLeafServer(this.port, channelInitializer);
        }
    }
}
