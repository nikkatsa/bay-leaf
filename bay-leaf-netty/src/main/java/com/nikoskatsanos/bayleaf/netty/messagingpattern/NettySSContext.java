package com.nikoskatsanos.bayleaf.netty.messagingpattern;

import com.nikoskatsanos.bayleaf.core.Session;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec;
import com.nikoskatsanos.bayleaf.core.message.ApplicationMessage;
import com.nikoskatsanos.bayleaf.core.message.MessageType;
import com.nikoskatsanos.bayleaf.core.messagingpattern.MessagingPattern;
import com.nikoskatsanos.bayleaf.core.messagingpattern.SSContext;
import com.nikoskatsanos.bayleaf.core.messagingpattern.SharedSubscription;
import com.nikoskatsanos.bayleaf.core.messagingpattern.SharedSubscriptionData;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettySSContext<SUBSCRIPTION, DATA> implements SSContext<SUBSCRIPTION, DATA> {

    private static final NettyJsonCodec NETTY_JSON_CODEC = NettyJsonCodec.instance();

    @Setter
    private Session session;
    @Setter
    private String serviceName;
    @Setter
    private String route;
    @Setter
    private BayLeafCodec.Serializer serializer;
    @Setter
    private BayLeafCodec.Deserializer deserializer;

    @Setter
    private ChannelHandlerContext channelCtx;

    private volatile Consumer<SharedSubscription<SUBSCRIPTION>> subscriptionConsumer;
    private volatile Consumer<SharedSubscription<SUBSCRIPTION>> closeConsumer;

    private final Map<String, String> subscriptionHashes = new HashMap<>();

    @Override
    public Session session() {
        return this.session;
    }

    @Override
    public void onSubscription(final Consumer<SharedSubscription<SUBSCRIPTION>> sharedSubscriptionConsumer) {
        this.subscriptionConsumer = sharedSubscriptionConsumer;
    }

    public void subscription(final ApplicationMessage appMsg) {
        final SUBSCRIPTION subscription = this.deserializer.deserialize(appMsg.getData());
        final String subscriptionHash = String.valueOf(subscription.hashCode());

        if (subscriptionHashes.containsKey(subscriptionHash)) {
            logger.warn("Session={} already has a Subscription={}. Ignoring duplicate.", this.session, subscription);
            return;
        } else {
            subscriptionHashes.put(subscriptionHash, appMsg.getCorrelationId());
        }

        final SharedSubscription<SUBSCRIPTION> sharedSubscription = new SharedSubscription<>(subscriptionHash, subscription);

        final NettyStreamContext<SUBSCRIPTION, DATA> sharedStreamContext = NettyStreamContext.create(subscription, this.serviceName, this.route, this.serializer);
        sharedStreamContext.addChannelContext(appMsg.getCorrelationId(), this.channelCtx);
        if (Objects.nonNull(this.subscriptionConsumer)) {
            this.subscriptionConsumer.accept(sharedSubscription);
        }
    }

    @Override
    public void onSubscriptionClose(final Consumer<SharedSubscription<SUBSCRIPTION>> closeConsumer) {
        this.closeConsumer = closeConsumer;
    }

    public void close(final ApplicationMessage appMsg) {
        if (Objects.nonNull(this.closeConsumer)) {
            final SUBSCRIPTION subscription = this.deserializer.deserialize(appMsg.getData());

            final String subscriptionHash = String.valueOf(subscription.hashCode());
            final String removedSubscriptionId = subscriptionHashes.remove(subscriptionHash);
            if (Objects.isNull(removedSubscriptionId)) {
                logger.warn("No active subscription found for Session={}, Subscription={}. Ignoring close request", this.session, subscription);
                return;
            }
            final SharedSubscription<SUBSCRIPTION> sharedSubscription = new SharedSubscription<>(subscriptionHash, subscription);

            final NettyStreamContext<SUBSCRIPTION, DATA> sharedStreamContext = NettyStreamContext.create(subscription, this.serviceName, this.route, this.serializer);
            sharedStreamContext.removeChannelContext(removedSubscriptionId, this.channelCtx);

            this.closeConsumer.accept(sharedSubscription);
        }
    }

    @Override
    public void snapshot(final SharedSubscriptionData<SUBSCRIPTION, DATA> snapshot) {
        final String correlationId = this.subscriptionHashes.get(snapshot.getSubscription().getSubscriptionId());
        final byte[] serialized = this.serializer.serialize(snapshot.getData());
        final ApplicationMessage appMsg = new ApplicationMessage(correlationId, MessageType.INITIAL_DATA, this.serviceName, this.route, MessagingPattern.SS, serialized);
        this.channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(appMsg)));
    }

    @Override
    public StreamContext<SUBSCRIPTION, DATA> streamContext(final SUBSCRIPTION subscription) {
        final NettyStreamContext<SUBSCRIPTION, DATA> sharedStreamContext = NettyStreamContext.create(subscription, this.serviceName, this.route, this.serializer);
        return sharedStreamContext;
    }

    @RequiredArgsConstructor
    private static class NettyStreamContext<SUBSCRIPTION, DATA> implements SSContext.StreamContext<SUBSCRIPTION, DATA> {

        private static final Map<String, NettyStreamContext<?, ?>> SHARED_STREAM_CONTEXTS = new ConcurrentHashMap<>();

        private final List<SubscriptionChannelHandlerContext> channelContexts = new CopyOnWriteArrayList<>();
        private final List<Consumer<Void>> closeConsumers = new CopyOnWriteArrayList<>();

        private final SUBSCRIPTION subscription;
        private final String serviceName;
        private final String route;
        private final BayLeafCodec.Serializer serializer;

        static <SUBSCRIPTION, DATA> NettyStreamContext<SUBSCRIPTION, DATA> create(
            final SUBSCRIPTION subscription,
            final String serviceName,
            final String route,
            final BayLeafCodec.Serializer serializer) {
            return (NettyStreamContext<SUBSCRIPTION, DATA>) SHARED_STREAM_CONTEXTS.computeIfAbsent(String.format("%s_%s_%d", serviceName, route, subscription.hashCode()),
                k -> new NettyStreamContext<>(subscription, serviceName, route, serializer));
        }

        @Override
        public SUBSCRIPTION subscription() {
            return this.subscription;
        }

        @Override
        public void onClose(final Consumer<Void> closeConsumer) {
            this.closeConsumers.add(closeConsumer);
        }

        @Override
        public void stream(final SharedSubscriptionData<SUBSCRIPTION, DATA> stream) {
            final byte[] serialized = serializer.serialize(stream.getData());
            for (final SubscriptionChannelHandlerContext subscriber : this.channelContexts) {
                final ApplicationMessage appMsg = new ApplicationMessage(subscriber.subscriptionId, MessageType.DATA, serviceName, route, MessagingPattern.SS, serialized);
                try {
                    final TextWebSocketFrame out = new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(appMsg));
                    subscriber.channelCtx.executor().execute( ()-> subscriber.channelCtx.writeAndFlush(out));
                } catch (final Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        void addChannelContext(final String subscriptionId, final ChannelHandlerContext channelCtx) {
            this.channelContexts.add(new SubscriptionChannelHandlerContext(subscriptionId, channelCtx));
        }

        void removeChannelContext(final String subscriptionId, final ChannelHandlerContext channelCtx) {
            this.channelContexts.remove(new SubscriptionChannelHandlerContext(subscriptionId, channelCtx));
            SHARED_STREAM_CONTEXTS.computeIfPresent(String.format("%s_%s_%s", this.serviceName, this.route, this.subscription.hashCode()), (k, v) -> {
                if (this.channelContexts.size() == 0) {
                    this.close();
                    return null;
                }
                return v;
            });
        }

        void close() {
            this.closeConsumers.forEach(c -> {
                try {
                    c.accept(null);
                } catch (final Exception e) {}
            });
        }
    }

    @Data
    @RequiredArgsConstructor
    private static class SubscriptionChannelHandlerContext {

        private final String subscriptionId;
        private final ChannelHandlerContext channelCtx;
    }
}
