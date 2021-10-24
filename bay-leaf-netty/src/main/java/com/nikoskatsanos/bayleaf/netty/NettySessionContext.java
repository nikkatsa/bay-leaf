package com.nikoskatsanos.bayleaf.netty;

import com.nikoskatsanos.bayleaf.core.Heartbeat;
import com.nikoskatsanos.bayleaf.core.message.Message;
import com.nikoskatsanos.bayleaf.core.message.MessageType;
import com.nikoskatsanos.bayleaf.core.Session;
import com.nikoskatsanos.bayleaf.core.SessionContext;
import com.nikoskatsanos.bayleaf.core.message.SessionMessage;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class NettySessionContext implements SessionContext {

    private static final NettyJsonCodec JSON_CODEC = NettyJsonCodec.instance();

    private final Session session;
    private final ChannelHandlerContext channelCtx;

    private Consumer<Heartbeat> heartbeatConsumer;

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
    public void onHeartbeat(final Consumer<Heartbeat> heartbeatConsumer) {
        this.heartbeatConsumer = heartbeatConsumer;
    }

    public void heartbeatIn(final Heartbeat heartbeat) {
        if (Objects.nonNull(this.heartbeatConsumer)) {
            this.heartbeatConsumer.accept(heartbeat);
        }
    }
}
