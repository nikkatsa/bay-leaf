package com.nikoskatsanos.bayleaf.netty.messagingpattern;

import com.nikoskatsanos.bayleaf.core.Session;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec;
import com.nikoskatsanos.bayleaf.core.message.ApplicationMessage;
import com.nikoskatsanos.bayleaf.core.message.ErrorMessage;
import com.nikoskatsanos.bayleaf.core.message.Message;
import com.nikoskatsanos.bayleaf.core.message.MessageType;
import com.nikoskatsanos.bayleaf.core.messagingpattern.MessagingPattern;
import com.nikoskatsanos.bayleaf.core.messagingpattern.RRContext;
import com.nikoskatsanos.bayleaf.core.messagingpattern.Request;
import com.nikoskatsanos.bayleaf.core.messagingpattern.Response;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.function.Consumer;
import lombok.Setter;

public class NettyRRContext<REQUEST, RESPONSE> implements RRContext<REQUEST, RESPONSE> {

    private static final NettyJsonCodec JSON_CODEC = NettyJsonCodec.instance();

    @Setter
    private Session session;
    @Setter
    private String serviceName;
    @Setter
    private String route;

    private Consumer<Request<REQUEST>> requestConsumer;

    @Setter
    private ChannelHandlerContext channelContext;

    @Setter
    private BayLeafCodec.Serializer serializer;
    @Setter
    private BayLeafCodec.Deserializer deserializer;

    @Override
    public Session session() {
        return this.session;
    }

    @Override
    public void onRequest(final Consumer<Request<REQUEST>> requestConsumer) {
        this.requestConsumer = requestConsumer;
    }

    public void request(final ApplicationMessage applicationMessage) {
        REQUEST deserialize = this.deserializer.deserialize(applicationMessage.getData());
        this.requestConsumer.accept(new Request<>(applicationMessage.getCorrelationId(), deserialize));
    }

    @Override
    public void response(final Response<REQUEST, RESPONSE> response) {
        final byte[] data = this.serializer.serialize(response.getResponse());
        final Message msg = new ApplicationMessage(response.getRequest().getId(), MessageType.DATA, this.serviceName, this.route, MessagingPattern.RR, data);
        this.channelContext.executor().execute(()-> this.channelContext.writeAndFlush(new TextWebSocketFrame(JSON_CODEC.serializeToString(msg))));
    }

    @Override
    public void error(final int errorCode, final String errorMsg, final Response<REQUEST, RESPONSE> errorResponse) {
        final Message msg = new ErrorMessage(errorCode, errorMsg, errorResponse.getRequest().getId(), this.serviceName, this.route, MessagingPattern.RR);
        this.channelContext.executor().execute(() -> this.channelContext.writeAndFlush(new TextWebSocketFrame(JSON_CODEC.serializeToString(msg))));
    }
}
