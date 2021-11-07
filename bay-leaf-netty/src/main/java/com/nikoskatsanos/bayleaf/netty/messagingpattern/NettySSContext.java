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
import com.nikoskatsanos.bayleaf.netty.dispatch.DispatchingStrategy.Dispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class NettySSContext<SUBSCRIPTION, DATA> implements SSContext<SUBSCRIPTION, DATA> {

    private static final NettyJsonCodec NETTY_JSON_CODEC = NettyJsonCodec.instance();

    private final Session session;
    private final String serviceName;
    private final String route;

    private final ChannelHandlerContext channelCtx;

    private final Dispatcher dispatcher;

    private BayLeafCodec.Serializer serializer;
    private Class<DATA> dataType;
    private BayLeafCodec.Deserializer deserializer;
    private Class<SUBSCRIPTION> subscriptionType;

    private volatile Consumer<SharedSubscription<SUBSCRIPTION>> subscriptionConsumer;
    private volatile Consumer<SharedSubscription<SUBSCRIPTION>> closeConsumer;

    private final Map<String, String> subscriptionHashes = new HashMap<>();
    private final List<SharedSubscription<SUBSCRIPTION>> activeSSSubscriptions = new ArrayList<>();

    @Override
    public Session session() {
        return this.session;
    }

    @Override
    public void onSubscription(final Consumer<SharedSubscription<SUBSCRIPTION>> sharedSubscriptionConsumer) {
        this.subscriptionConsumer = sharedSubscriptionConsumer;
    }

    public void subscription(final ApplicationMessage appMsg) {
        final SUBSCRIPTION subscription = this.deserializer.<SUBSCRIPTION>deserialize(appMsg.getData(), this.subscriptionType);
        final String subscriptionHash = String.valueOf(subscription.hashCode());

        if (subscriptionHashes.containsKey(subscriptionHash)) {
            logger.warn("Session={} already has a Subscription={}. Ignoring duplicate.", this.session, subscription);
            return;
        } else {
            subscriptionHashes.put(subscriptionHash, appMsg.getCorrelationId());
        }

        final SharedSubscription<SUBSCRIPTION> sharedSubscription = new SharedSubscription<>(subscriptionHash, subscription);

        final NettyStreamContext<SUBSCRIPTION, DATA> sharedStreamContext = NettyStreamContext.create(subscription, this.serviceName, this.route, this.serializer, this.dataType);
        sharedStreamContext.addChannelContext(appMsg.getCorrelationId(), this.channelCtx);
        if (Objects.nonNull(this.subscriptionConsumer)) {
            this.activeSSSubscriptions.add(sharedSubscription);
            this.dispatcher.dispatch(() -> this.subscriptionConsumer.accept(sharedSubscription));
        }
    }

    @Override
    public void onSubscriptionClose(final Consumer<SharedSubscription<SUBSCRIPTION>> closeConsumer) {
        this.closeConsumer = closeConsumer;
    }

    public void close(final ApplicationMessage appMsg) {
        if (Objects.nonNull(this.closeConsumer)) {
            final SUBSCRIPTION subscription = this.deserializer.deserialize(appMsg.getData(), this.subscriptionType);

            final String subscriptionHash = String.valueOf(subscription.hashCode());
            final String removedSubscriptionId = subscriptionHashes.remove(subscriptionHash);
            if (Objects.isNull(removedSubscriptionId)) {
                logger.warn("No active subscription found for Session={}, Subscription={}. Ignoring close request", this.session, subscription);
                return;
            }
            final SharedSubscription<SUBSCRIPTION> sharedSubscription = new SharedSubscription<>(subscriptionHash, subscription);

            final NettyStreamContext<SUBSCRIPTION, DATA> sharedStreamContext = NettyStreamContext.create(subscription, this.serviceName, this.route, this.serializer, this.dataType);
            sharedStreamContext.removeChannelContext(removedSubscriptionId, this.channelCtx);

            this.activeSSSubscriptions.remove(sharedSubscription);
            this.dispatcher.dispatch(() -> this.closeConsumer.accept(sharedSubscription));
        }
    }

    public void destroy() {
        logger.info("Destroying {} for Session={}", NettySSContext.class.getSimpleName(), this.session);
        this.activeSSSubscriptions.forEach(sub -> this.dispatcher.dispatch(() -> this.closeConsumer.accept(sub)));
        this.activeSSSubscriptions.clear();
    }

    @Override
    public void snapshot(final SharedSubscriptionData<SUBSCRIPTION, DATA> snapshot) {
        final String correlationId = this.subscriptionHashes.get(snapshot.getSubscription().getSubscriptionId());
        final byte[] serialized = this.serializer.serialize(snapshot.getData(), this.dataType);
        final ApplicationMessage appMsg = new ApplicationMessage(correlationId, MessageType.INITIAL_DATA, this.serviceName, this.route, MessagingPattern.SS, serialized);
        this.channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(appMsg)));
    }

    @Override
    public StreamContext<SUBSCRIPTION, DATA> streamContext(final SUBSCRIPTION subscription) {
        final NettyStreamContext<SUBSCRIPTION, DATA> sharedStreamContext = NettyStreamContext.create(subscription, this.serviceName, this.route, this.serializer, this.dataType);
        return sharedStreamContext;
    }

    public void setSerializer(final BayLeafCodec.Serializer serializer, final Class<SUBSCRIPTION> inType) {
        this.serializer = serializer;
        this.subscriptionType = inType;
    }

    public void setDeserializer(final BayLeafCodec.Deserializer deserializer, final Class<DATA> outType) {
        this.deserializer = deserializer;
        this.dataType = outType;
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
        private final Class<DATA> dataType;

        static <SUBSCRIPTION, DATA> NettyStreamContext<SUBSCRIPTION, DATA> create(
            final SUBSCRIPTION subscription,
            final String serviceName,
            final String route,
            final BayLeafCodec.Serializer serializer,
            final Class<DATA> dataType) {
            return (NettyStreamContext<SUBSCRIPTION, DATA>) SHARED_STREAM_CONTEXTS.computeIfAbsent(String.format("%s_%s_%d", serviceName, route, subscription.hashCode()),
                k -> new NettyStreamContext<>(subscription, serviceName, route, serializer, dataType));
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
            final byte[] serialized = serializer.serialize(stream.getData(), this.dataType);
            for (final SubscriptionChannelHandlerContext subscriber : this.channelContexts) {
                final ApplicationMessage appMsg = new ApplicationMessage(subscriber.subscriptionId, MessageType.DATA, serviceName, route, MessagingPattern.SS, serialized);
                try {
                    final TextWebSocketFrame out = new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(appMsg));
                    subscriber.channelCtx.executor().execute(() -> subscriber.channelCtx.writeAndFlush(out));
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
                } catch (final Exception e) {
                }
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
