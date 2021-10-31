package com.nikoskatsanos.bayleaf.netty.messagingpattern;

import com.nikoskatsanos.bayleaf.core.Session;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec;
import com.nikoskatsanos.bayleaf.core.message.ApplicationMessage;
import com.nikoskatsanos.bayleaf.core.message.MessageType;
import com.nikoskatsanos.bayleaf.core.messagingpattern.MessagingPattern;
import com.nikoskatsanos.bayleaf.core.messagingpattern.PSContext;
import com.nikoskatsanos.bayleaf.core.messagingpattern.Subscription;
import com.nikoskatsanos.bayleaf.core.messagingpattern.SubscriptionData;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import com.nikoskatsanos.bayleaf.netty.dispatch.DispatchingStrategy.Dispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class NettyPSContext<SUBSCRIPTION, DATA> implements PSContext<SUBSCRIPTION, DATA> {

    private static final NettyJsonCodec JSON_CODEC = NettyJsonCodec.instance();

    private final Session session;
    private final String serviceName;
    private final String route;

    private final ChannelHandlerContext channelContext;

    private final Dispatcher dispatcher;

    private BayLeafCodec.Serializer serializer;
    private Class<DATA> dataType;
    private BayLeafCodec.Deserializer deserializer;
    private Class<SUBSCRIPTION> subscriptionType;

    private volatile Consumer<Subscription<SUBSCRIPTION>> subscriptionConsumer;
    private volatile Consumer<Subscription<SUBSCRIPTION>> closeConsumer;

    @Override
    public Session session() {
        return this.session;
    }

    @Override
    public void onSubscription(final Consumer<Subscription<SUBSCRIPTION>> subscriptionConsumer) {
        this.subscriptionConsumer = subscriptionConsumer;
    }

    public void subscription(final ApplicationMessage applicationMessage) {
        final SUBSCRIPTION subscriptionData = this.deserializer.deserialize(applicationMessage.getData(), this.subscriptionType);
        final Subscription<SUBSCRIPTION> subscription = new Subscription<>(applicationMessage.getCorrelationId(), subscriptionData);
        if (Objects.nonNull(this.subscriptionConsumer)) {
            this.dispatcher.dispatch(() -> this.subscriptionConsumer.accept(subscription));
        } else {
            logger.warn("Subscription consumer not set for Service={}, Route={}, Session={}. Discarding Subscription={}", this.serviceName, this.route, this.session, subscription);
        }
    }

    @Override
    public void onClose(final Consumer<Subscription<SUBSCRIPTION>> closeConsumer) {
        this.closeConsumer = closeConsumer;
    }

    public void close(final ApplicationMessage appMsg) {
        if (Objects.nonNull(this.closeConsumer)) {
            final SUBSCRIPTION subscriptionData = this.deserializer.deserialize(appMsg.getData(), this.subscriptionType);
            final Subscription<SUBSCRIPTION> subscription = new Subscription<>(appMsg.getCorrelationId(), subscriptionData);
            this.dispatcher.dispatch(() -> this.closeConsumer.accept(subscription));
        }
    }

    @Override
    public void initialData(final SubscriptionData<SUBSCRIPTION, DATA> snapshot) {
        this.sendData(snapshot, MessageType.INITIAL_DATA);
    }

    @Override
    public void data(final SubscriptionData<SUBSCRIPTION, DATA> subscriptionData) {
        this.sendData(subscriptionData, MessageType.DATA);
    }

    private void sendData(final SubscriptionData<SUBSCRIPTION, DATA> subscriptionData, final MessageType msgType) {
        final byte[] serialized = this.serializer.serialize(subscriptionData.getData(), this.dataType);
        final ApplicationMessage applicationMessage =
            new ApplicationMessage(subscriptionData.getSubscription().getId(), msgType, this.serviceName, this.route, MessagingPattern.PS, serialized);
        this.channelContext.executor().execute(() -> this.channelContext.writeAndFlush(new TextWebSocketFrame(JSON_CODEC.serializeToString(applicationMessage))));
    }

    public void setSerializer(final BayLeafCodec.Serializer serializer, final Class<SUBSCRIPTION> inType) {
        this.serializer = serializer;
        this.subscriptionType = inType;
    }

    public void setDeserializer(final BayLeafCodec.Deserializer deserializer, final Class<DATA> outType) {
        this.deserializer = deserializer;
        this.dataType = outType;
    }
}
