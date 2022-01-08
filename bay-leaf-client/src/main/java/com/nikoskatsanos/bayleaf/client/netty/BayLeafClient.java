package com.nikoskatsanos.bayleaf.client.netty;

import com.nikoskatsanos.bayleaf.client.SessionCallback;
import com.nikoskatsanos.bayleaf.client.auth.ClientAuthTokenProvider;
import com.nikoskatsanos.bayleaf.client.netty.handler.BayLeafApplicationMessageHandler;
import com.nikoskatsanos.bayleaf.client.netty.handler.BayLeafClientSessionHandler;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.TrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class BayLeafClient implements AutoCloseable {

    private final String host;
    private final int port;

    private final File sslTrustStore;
    private final char[] sslTrustStorePass;

    private final ClientAuthTokenProvider authTokenProvider;

    private NioEventLoopGroup eventLoop;

    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    private BayLeafClientSessionHandler sessionHandler;
    private final CompletableFuture<Void> sessionInitializedFuture = new CompletableFuture<>();
    private BayLeafApplicationMessageHandler applicationMessageHandler;

    private final Map<String, BayLeafServiceNettyImpl> services = new ConcurrentHashMap<>();

    public Future<Void> start(final SessionCallback sessionCallback) {
        logger.info("Starting {} to Remote={}:{}", BayLeafClient.class, this.host, this.port);

        if (!this.isStarted.compareAndSet(false, true)) {
            throw new RuntimeException(String.format("BayLeaf client already started to Remote=%s:%s", this.host, this.port));
        }

        this.sessionHandler = new BayLeafClientSessionHandler(this.authTokenProvider, sessionCallback);
        this.sessionHandler.setOnSessionInitialized(v -> this.sessionInitializedFuture.complete(null));
        this.applicationMessageHandler = new BayLeafApplicationMessageHandler();
        this.eventLoop = new NioEventLoopGroup(1);
        final Bootstrap client = new Bootstrap()
            .channel(NioSocketChannel.class)
            .group(eventLoop)
            .handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(final NioSocketChannel channel) throws Exception {
                    final ChannelPipeline pipeline = channel.pipeline();

                    if (Objects.nonNull(sslTrustStore)) {
                        if (!sslTrustStore.exists()) {
                            throw new RuntimeException(String.format("Provided SSL TrustStore=%s does not exist", sslTrustStore));
                        }
                        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                        final KeyStore trustStore = KeyStore.getInstance("JKS");
                        trustStore.load(new FileInputStream(sslTrustStore), sslTrustStorePass);
                        trustManagerFactory.init(trustStore);
                        final SslContext sslContext = SslContextBuilder.forClient().trustManager(trustManagerFactory).build();

                        pipeline.addLast(sslContext.newHandler(channel.alloc()));
                    }

                    pipeline.addLast(new HttpClientCodec(512, 512, 512));
                    pipeline.addLast(new HttpObjectAggregator(16_384));
                    final String protocol = Objects.nonNull(sslTrustStore) ? "wss" : "ws";
                    final WebSocketClientHandshaker13 wsHandshaker = new WebSocketClientHandshaker13(new URI(String.format("%s://%s:%d",protocol, host, port)),
                        WebSocketVersion.V13, "", false, new DefaultHttpHeaders(false), 64_000);
                    pipeline.addLast(new WebSocketClientProtocolHandler(wsHandshaker));
                    pipeline.addLast(sessionHandler);
                    pipeline.addLast(applicationMessageHandler);
                }
            });
        client.connect(new InetSocketAddress(this.host, this.port)).syncUninterruptibly();

        return this.sessionInitializedFuture;
    }

    public BayLeafServiceNettyImpl createService(final String serviceName, final BayLeafCodec.Serializer serializer, final BayLeafCodec.Deserializer deserializer) {
        final ChannelHandlerContext channelContext = this.sessionHandler.getChannelContext();

        final BayLeafServiceNettyImpl bayLeafService = this.services.computeIfAbsent(serviceName, k -> new BayLeafServiceNettyImpl(k, serializer, deserializer, channelContext));
        this.sessionHandler.registerService(bayLeafService);
        this.applicationMessageHandler.registerService(bayLeafService);

        bayLeafService.start();
        return bayLeafService;
    }

    @Override
    public void close() throws Exception {
        logger.info("Stopping {} to Remote={}:{}", BayLeafClient.class, this.host, this.port);

        if (Objects.nonNull(this.sessionHandler)) {
            this.sessionHandler.close();
        }
        if (Objects.nonNull(this.eventLoop)) {
            this.eventLoop.shutdownGracefully().awaitUninterruptibly();
        }
        logger.info("Stopped {} to Remote={}:{}", BayLeafClient.class, this.host, this.port);
    }

    public static BayLeafClient.Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String host;
        private int port;

        private File sslTrustStore;
        private char[] trustStorePass;

        private ClientAuthTokenProvider authTokenProvider;

        /**
         * @param host where remote BayLeaf server is running
         * @return same instance of {@link Builder} for chaining method calls
         */
        public Builder withHost(final String host) {
            this.host = host;
            return this;
        }

        /**
         * @param port where remote BayLeaf server is running
         * @return same instance of {@link Builder} for chaining method calls
         */
        public Builder withPort(final int port) {
            this.port = port;
            return this;
        }

        /**
         * @param trustStore containing certificate of remote BayLeaf server as a trusted source
         * @param trustStorePass password for truststore
         * @return same instance of {@link Builder} for chaining method calls
         */
        public Builder withSSLTrustStore(final File trustStore, final char[] trustStorePass) {
            this.sslTrustStore = trustStore;
            this.trustStorePass = trustStorePass;
            return this;
        }

        /**
         * @param authTokenProvider to be used during BayLeaf session initialization phase, in order to provide client's credentials for authentication/authorization by the server
         * @return same instance of {@link Builder} for chaining method calls
         */
        public Builder withAuthTokenProvider(final ClientAuthTokenProvider authTokenProvider) {
            this.authTokenProvider = authTokenProvider;
            return this;
        }

        /**
         * @return {@link BayLeafClient} constructed based on the parameters passed to the {@link Builder}
         */
        public BayLeafClient build() {
            Objects.requireNonNull(this.authTokenProvider, "AuthTokenProvider must be supplied for creating a BayLeaf  client");
            return new BayLeafClient(host, port, sslTrustStore, this.trustStorePass, this.authTokenProvider);
        }
    }
}
