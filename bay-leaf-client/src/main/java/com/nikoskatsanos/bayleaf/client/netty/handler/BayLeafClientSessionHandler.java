package com.nikoskatsanos.bayleaf.client.netty.handler;

import com.nikoskatsanos.bayleaf.client.SessionCallback;
import com.nikoskatsanos.bayleaf.client.auth.ClientAuthTokenProvider;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafServiceNettyImpl;
import com.nikoskatsanos.bayleaf.domain.message.Heartbeat;
import com.nikoskatsanos.bayleaf.domain.message.ApplicationMessage;
import com.nikoskatsanos.bayleaf.domain.message.Message;
import com.nikoskatsanos.bayleaf.domain.message.MessageType;
import com.nikoskatsanos.bayleaf.domain.message.SessionMessage;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Sharable
public class BayLeafClientSessionHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final NettyJsonCodec NETTY_JSON_CODEC = NettyJsonCodec.instance();

    private final ClientAuthTokenProvider authTokenProvider;
    private final SessionCallback sessionCallback;
    @Setter
    private Consumer<Void> onSessionInitialized;

    private volatile ChannelHandlerContext channelCtx;

    private final Map<String, BayLeafServiceNettyImpl> services = new ConcurrentHashMap<>();

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt instanceof WebSocketClientProtocolHandler.ClientHandshakeStateEvent) {
            WebSocketClientProtocolHandler.ClientHandshakeStateEvent handshakeEvent = (WebSocketClientProtocolHandler.ClientHandshakeStateEvent)evt;
            logger.info("WS HandshakeEvent={}", handshakeEvent);
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final TextWebSocketFrame msg) throws Exception {
        final Message message = NETTY_JSON_CODEC.deserializeFromString(msg.text(), Message.class);

        if (message instanceof ApplicationMessage) {
            super.channelRead(ctx, (ApplicationMessage)message);
            return;
        }

        switch (message.getMessageType()) {
            case SESSION_INITIALIZING:
                final SessionMessage sessionInitializing = (SessionMessage) message;
                logger.info("On SessionInitializing={}", sessionInitializing);
                final String sessionId = sessionInitializing.getCorrelationId();
                final SessionMessage auth = new SessionMessage(sessionId, MessageType.AUTH, NETTY_JSON_CODEC.serializeToBytes(this.authTokenProvider.authToken()));
                ctx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(auth)));
                break;
            case SESSION_INITIALIZED:
                this.sessionInitialized(ctx);
                break;
            case HEARTBEAT:
                final SessionMessage heartbeatMsg = (SessionMessage) message;
                final Heartbeat heartbeat = NETTY_JSON_CODEC.deserializeFromBytes(heartbeatMsg.getData(), Heartbeat.class);
                final BayLeafServiceNettyImpl bayLeafService = this.services.get(heartbeat.getServiceName());
                if (Objects.isNull(bayLeafService)) {
                    logger.warn("Received heartbeat for unknown Service={}, Heartbeat={}", heartbeat.getServiceName(), heartbeat);
                    return;
                }

                bayLeafService.onHeartbeat(heartbeat);
                break;
            default:
                logger.warn("Unhandled Message={}", msg.text());
        }
    }

    private void sessionInitialized(final ChannelHandlerContext ctx) {
        this.channelCtx = ctx;

        this.channelCtx.channel().closeFuture().addListener(c -> {
            this.sessionCallback.onSessionDestroyed();
        });

        this.sessionCallback.onSessionInitialized();
        if (Objects.nonNull(this.onSessionInitialized)) {
            this.onSessionInitialized.accept(null);
        }
    }

    public ChannelHandlerContext getChannelContext() {
        return this.channelCtx;
    }

    public void registerService(final BayLeafServiceNettyImpl service) {
        services.putIfAbsent(service.getServiceName(), service);
    }

    public void close() {
        if (Objects.nonNull(this.channelCtx)) {
            this.channelCtx.channel().close().syncUninterruptibly();
        }
    }
}
