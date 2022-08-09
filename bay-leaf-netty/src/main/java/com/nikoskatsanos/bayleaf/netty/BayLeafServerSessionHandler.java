package com.nikoskatsanos.bayleaf.netty;

import com.nikoskatsanos.bayleaf.domain.Session;
import com.nikoskatsanos.bayleaf.domain.User;
import com.nikoskatsanos.bayleaf.core.auth.Authenticator;
import com.nikoskatsanos.bayleaf.core.auth.Authenticator.Token;
import com.nikoskatsanos.bayleaf.core.auth.Authorizer;
import com.nikoskatsanos.bayleaf.core.auth.Tokens;
import com.nikoskatsanos.bayleaf.domain.message.Message;
import com.nikoskatsanos.bayleaf.domain.message.MessageType;
import com.nikoskatsanos.bayleaf.domain.message.SessionMessage;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Sharable
public class BayLeafServerSessionHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final NettyJsonCodec JSON_CODEC = NettyJsonCodec.instance();

    private final Authenticator authenticator;
    private final Authorizer authorizer;

    private final Map<ChannelId, Session> sessions = new ConcurrentHashMap<>(64);
    private final Map<ChannelId, ScheduledFuture> initializingSessions = new ConcurrentHashMap<>();

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            final Session session = new Session(UUID.randomUUID().toString());
            sessions.put(ctx.channel().id(), session);

            ctx.channel().closeFuture().addListener(c -> {
                final Session closedSession = this.sessions.remove(ctx.channel().id());
                logger.info("OnSessionTerminated Session={}, Address={}", closedSession, ctx.channel().remoteAddress());
            });

            final Message msg = new SessionMessage(session.getSessionId(), MessageType.SESSION_INITIALIZING, null);
            final String msgSerialized = JSON_CODEC.serializeToString(msg);
            logger.info("OnSessionInitializing Session={}, Address={}", session, ctx.channel().remoteAddress());
            ctx.channel().attr(AttributeKey.valueOf(NettyProps.NETTY_SESSION_ID)).set(session);

            final ScheduledFuture<?> channelInitializedValidationTask = ctx.executor()
                .schedule(new SessionInitializingTimeoutAction(ctx.channel(), session), NettyProps.SESSION_INITIALIZED_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            this.initializingSessions.put(ctx.channel().id(), channelInitializedValidationTask);

            ctx.writeAndFlush(new TextWebSocketFrame(msgSerialized));

            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final TextWebSocketFrame msg) throws Exception {
        final String text = msg.text();
        logger.debug("OnMessageReceived={}", text);

        final Message message = JSON_CODEC.deserializeFromString(text, Message.class);
        switch (message.getMessageType()) {
            case AUTH:
                final Session session = this.sessions.get(ctx.channel().id());
                if (Objects.isNull(session)) {
                    ctx.channel().close();
                    return;
                }
                if (session.isAuthenticated()) {
                    logger.warn("Already authenticated Session={}", session);
                    return;
                }

                final byte[] data = message.getData();
                final Token token = JSON_CODEC.deserializeFromBytes(data, Tokens.UsernamePassword.class);
                final User user = this.authenticator.authenticate(token);
                if (Objects.isNull(user)) {
                    logger.error("Failed to authenticate Session={}, Channel={}, Token={}", session, ctx.channel().id(), token);
                    ctx.channel().close();
                    return;
                }
                final String[] roles = this.authorizer.authorize(user);
                user.addRoles(Arrays.asList(roles));
                session.setUser(user);

                final ScheduledFuture sessionInitializedValidationTask = this.initializingSessions.remove(ctx.channel().id());
                if (Objects.nonNull(sessionInitializedValidationTask)) {
                    sessionInitializedValidationTask.cancel(true);
                }

                final Message sessionInitialized = new SessionMessage(session.getSessionId(), MessageType.SESSION_INITIALIZED, null);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(JSON_CODEC.serializeToString(sessionInitialized)));

                logger.info("OnSessionInitialized Session={}, Address={}", session, ctx.channel().remoteAddress());
                break;
            default:
                super.channelRead(ctx, message);
        }
    }

    @RequiredArgsConstructor
    private class SessionInitializingTimeoutAction implements Runnable {
        private final Channel channel;
        private final Session session;

        @Override
        public void run() {
            if (Objects.nonNull(initializingSessions.remove(this.channel.id()))) {
                logger.warn("Session={} did not initialized after {}ms. Closing Channel={}", this.session, NettyProps.SESSION_INITIALIZED_TIMEOUT_MS, this.channel.id());
                sessions.remove(this.channel.id());

                if (Objects.nonNull(this.channel) && this.channel.isOpen()) {
                    this.channel.close();
                }
            }
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (cause.getCause() instanceof SSLHandshakeException) {
            logger.warn("SSLHandskake Warning={}", cause.getCause().getMessage());
            return;
        }
        super.exceptionCaught(ctx, cause);
    }
}
