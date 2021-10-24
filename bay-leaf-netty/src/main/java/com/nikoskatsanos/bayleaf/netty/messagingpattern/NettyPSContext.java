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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyPSContext<SUBSCRIPTION, DATA> implements PSContext<SUBSCRIPTION, DATA> {

    private static final NettyJsonCodec JSON_CODEC = NettyJsonCodec.instance();

    @Setter
    private Session session;
    @Setter
    private String serviceName;
    @Setter
    private String route;

    @Setter
    private ChannelHandlerContext channelContext;

    @Setter
    private BayLeafCodec.Serializer serializer;
    @Setter
    private BayLeafCodec.Deserializer deserializer;

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
        final SUBSCRIPTION subscriptionData = this.deserializer.deserialize(applicationMessage.getData());
        final Subscription<SUBSCRIPTION> subscription = new Subscription<>(applicationMessage.getCorrelationId(), subscriptionData);
        if (Objects.nonNull(this.subscriptionConsumer)) {
            this.subscriptionConsumer.accept(subscription);
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
            final SUBSCRIPTION subscriptionData = this.deserializer.deserialize(appMsg.getData());
            final Subscription<SUBSCRIPTION> subscription = new Subscription<>(appMsg.getCorrelationId(), subscriptionData);
            this.closeConsumer.accept(subscription);
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
        final byte[] serialized = this.serializer.serialize(subscriptionData.getData());
        final ApplicationMessage applicationMessage =
            new ApplicationMessage(subscriptionData.getSubscription().getId(), msgType, this.serviceName, this.route, MessagingPattern.PS, serialized);
        this.channelContext.executor().execute(()-> this.channelContext.writeAndFlush(new TextWebSocketFrame(JSON_CODEC.serializeToString(applicationMessage))));
    }
}
