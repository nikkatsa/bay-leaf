package com.nikoskatsanos.bayleaf.netty;

import com.nikoskatsanos.bayleaf.domain.message.Heartbeat;
import com.nikoskatsanos.bayleaf.domain.message.Message;
import com.nikoskatsanos.bayleaf.domain.message.MessageType;
import com.nikoskatsanos.bayleaf.domain.Session;
import com.nikoskatsanos.bayleaf.server.SessionContext;
import com.nikoskatsanos.bayleaf.domain.message.SessionMessage;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class NettySessionContext implements SessionContext {

    private static final NettyJsonCodec JSON_CODEC = NettyJsonCodec.instance();

    private final Session session;
    private final ChannelHandlerContext channelCtx;

    private Map<String, Consumer<Heartbeat>> heartbeatConsumers = new ConcurrentHashMap<>();

    @Override
    public Session getSession() {
        return this.session;
    }

    @Override
    public void heartbeat(final Heartbeat heartbeat) {
        final Message heartbeatMsg = new SessionMessage(String.valueOf(heartbeat.getId()), MessageType.HEARTBEAT, JSON_CODEC.serializeToBytes(heartbeat));
        this.channelCtx.executor().execute(() -> this.channelCtx.writeAndFlush(new TextWebSocketFrame(JSON_CODEC.serializeToString(heartbeatMsg))));
    }

    @Override
    public void onHeartbeat(final String serviceName, final Consumer<Heartbeat> heartbeatConsumer) {
        this.heartbeatConsumers.put(serviceName, heartbeatConsumer);
    }

    @Override
    public void closeSession(final byte closeCode) {
        logger.info("Closing Session={}, CloseCode={}", this.session, closeCode);
        this.channelCtx.channel().close();
    }

    public void heartbeatIn(final Heartbeat heartbeat) {
        final Consumer<Heartbeat> heartbeatConsumer = this.heartbeatConsumers.get(heartbeat.getServiceName());
        if (Objects.nonNull(heartbeatConsumer)) {
            heartbeatConsumer.accept(heartbeat);
        }
    }
}
