package com.nikoskatsanos.bayleaf.netty.messagingpattern;

import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec;
import com.nikoskatsanos.bayleaf.core.message.ApplicationMessage;
import com.nikoskatsanos.bayleaf.core.message.MessageType;
import com.nikoskatsanos.bayleaf.core.messagingpattern.MessagingPattern;
import com.nikoskatsanos.bayleaf.server.messagingpattern.BCContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.Broadcast;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class NettyBCContext<BROADCAST> implements BCContext<BROADCAST> {

    private static final NettyJsonCodec JSON_CODEC = NettyJsonCodec.instance();

    private final String serviceName;
    private final String route;

    private BayLeafCodec.Serializer serializer;
    private Class<BROADCAST> broadcastType;

    private Set<ChannelHandlerContext> channelContexts = new HashSet<>();

    public void addChannelHandlerContext(final ChannelHandlerContext channelHandlerContext) {
        this.channelContexts.add(channelHandlerContext);
    }

    @Override
    public void broadcast(final Broadcast<BROADCAST> broadcast) {
        final byte[] data = this.serializer.serialize(broadcast.getBroadcastMsg(), this.broadcastType);
        final ApplicationMessage msg = new ApplicationMessage("", MessageType.DATA, this.serviceName, this.route, MessagingPattern.BC, data);
        final String serialized = JSON_CODEC.serializeToString(msg);
        for (final ChannelHandlerContext channelCtx : this.channelContexts) {
            try {
                channelCtx.executor().execute(() -> channelCtx.writeAndFlush(new TextWebSocketFrame(serialized)));
            } catch (final Exception e) {
                logger.error("Failed to broadcast Message={} to Channel={}", broadcast, channelCtx.channel(), e);
            }
        }
    }

    public void setSerializer(final BayLeafCodec.Serializer serializer, final Class<BROADCAST> broadcastType) {
        this.serializer = serializer;
        this.broadcastType = broadcastType;
    }
}
