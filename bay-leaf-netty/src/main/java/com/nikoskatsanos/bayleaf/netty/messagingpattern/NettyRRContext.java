package com.nikoskatsanos.bayleaf.netty.messagingpattern;

import com.nikoskatsanos.bayleaf.domain.Session;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec;
import com.nikoskatsanos.bayleaf.domain.message.ApplicationMessage;
import com.nikoskatsanos.bayleaf.domain.message.ErrorMessage;
import com.nikoskatsanos.bayleaf.domain.message.Message;
import com.nikoskatsanos.bayleaf.domain.message.MessageType;
import com.nikoskatsanos.bayleaf.domain.message.MessagingPattern;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RRContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.Request;
import com.nikoskatsanos.bayleaf.server.messagingpattern.Response;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import com.nikoskatsanos.bayleaf.netty.dispatch.DispatchingStrategy.Dispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NettyRRContext<REQUEST, RESPONSE> implements RRContext<REQUEST, RESPONSE> {

    private static final NettyJsonCodec JSON_CODEC = NettyJsonCodec.instance();

    private final Session session;
    private final String serviceName;
    private final String route;

    private final ChannelHandlerContext channelContext;

    private final Dispatcher dispatcher;

    private Consumer<Request<REQUEST>> requestConsumer;

    private BayLeafCodec.Serializer serializer;
    private Class<RESPONSE> responseType;
    private BayLeafCodec.Deserializer deserializer;
    private Class<REQUEST> requestType;

    @Override
    public Session session() {
        return this.session;
    }

    @Override
    public void onRequest(final Consumer<Request<REQUEST>> requestConsumer) {
        this.requestConsumer = requestConsumer;
    }

    public void request(final ApplicationMessage applicationMessage) {
        REQUEST deserialize = this.deserializer.deserialize(applicationMessage.getData(), this.requestType);
        this.dispatcher.dispatch(() -> this.requestConsumer.accept(new Request<>(applicationMessage.getCorrelationId(), deserialize)));
    }

    @Override
    public void response(final Response<REQUEST, RESPONSE> response) {
        final byte[] data = this.serializer.serialize(response.getResponse(), this.responseType);
        final Message msg = new ApplicationMessage(response.getRequest().getId(), MessageType.DATA, this.serviceName, this.route, MessagingPattern.RR, data);
        this.channelContext.executor().execute(() -> this.channelContext.writeAndFlush(new TextWebSocketFrame(JSON_CODEC.serializeToString(msg))));
    }

    @Override
    public void error(final int errorCode, final String errorMsg, final Response<REQUEST, RESPONSE> errorResponse) {
        final Message msg = new ErrorMessage(errorCode, errorMsg, errorResponse.getRequest().getId(), this.serviceName, this.route, MessagingPattern.RR);
        this.channelContext.executor().execute(() -> this.channelContext.writeAndFlush(new TextWebSocketFrame(JSON_CODEC.serializeToString(msg))));
    }

    public void setSerializer(final BayLeafCodec.Serializer serializer, final Class<RESPONSE> responseType) {
        this.serializer = serializer;
        this.responseType = responseType;
    }

    public void setDeserializer(final BayLeafCodec.Deserializer deserializer, final Class<REQUEST> requestType) {
        this.deserializer = deserializer;
        this.requestType = requestType;
    }
}
