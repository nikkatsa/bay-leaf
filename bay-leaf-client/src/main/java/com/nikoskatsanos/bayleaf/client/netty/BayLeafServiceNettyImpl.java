package com.nikoskatsanos.bayleaf.client.netty;

import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.StreamCallback;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.RRAPromise;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.RRAPromise.Ack;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.RRPromise;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.RequestAndPromiseRecord;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.RequestAndRRAPromiseRecord;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.SubscriptionAndStreamCallbackRecord;
import com.nikoskatsanos.bayleaf.core.Heartbeat;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec.Deserializer;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec.Serializer;
import com.nikoskatsanos.bayleaf.core.message.ApplicationMessage;
import com.nikoskatsanos.bayleaf.core.message.ErrorMessage;
import com.nikoskatsanos.bayleaf.core.message.MessageType;
import com.nikoskatsanos.bayleaf.core.message.ServiceMessage;
import com.nikoskatsanos.bayleaf.core.message.SessionMessage;
import com.nikoskatsanos.bayleaf.core.messagingpattern.MessagingPattern;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class BayLeafServiceNettyImpl implements BayLeafService{

    private static final NettyJsonCodec NETTY_JSON_CODEC = NettyJsonCodec.instance();

    @Getter
    private final String serviceName;
    private final BayLeafCodec.Serializer serializer;
    private final BayLeafCodec.Deserializer deserializer;
    private final ChannelHandlerContext channelCtx;

    private final Map<String, RRNettyImpl> rrRoutes = new ConcurrentHashMap<>();
    private final Map<String, RRANettyImpl> rraRoutes = new ConcurrentHashMap<>();
    private final Map<String, PSNettyImpl> psRoutes = new ConcurrentHashMap<>();
    private final Map<String, SSNettyImpl> ssRoutes = new ConcurrentHashMap<>();
    private final Map<String, BCNettyImpl> bcRoutes = new ConcurrentHashMap<>();

    @Override
    public void start() {
        logger.info("Starting BayLeafService={}", this.serviceName);
        final String correlationId = UUID.randomUUID().toString();
        final SessionMessage serviceCreate = new SessionMessage(correlationId, MessageType.SERVICE_CREATE, NETTY_JSON_CODEC.serializeToBytes(new ServiceMessage(correlationId, this.serviceName)));
        logger.info("ServiceCreate={}", serviceCreate);
        this.channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(serviceCreate))).syncUninterruptibly();
    }

    @Override
    public void close() throws Exception {
        logger.info("Closing BayLeafService={}", this.serviceName);
    }

    @Override
    public Serializer serializer() {
        return this.serializer;
    }

    @Override
    public Deserializer deserializer() {
        return this.deserializer;
    }

    @Override
    public void onHeartbeat(final Heartbeat heartbeat) {
        final Heartbeat heartbeatOut = new Heartbeat(this.serviceName, heartbeat.getId(), System.currentTimeMillis());
        final SessionMessage sessionMessage = new SessionMessage(String.valueOf(heartbeat.getId()), MessageType.HEARTBEAT, NETTY_JSON_CODEC.serializeToBytes(heartbeatOut));
        channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(sessionMessage)));
    }

    @Override
    public <REQUEST, RESPONSE> RR<REQUEST, RESPONSE> createRR(final String route, final Class<REQUEST> requestType, final Class<RESPONSE> responseType) {
        final RRNettyImpl<REQUEST, RESPONSE> rrRoute = this.rrRoutes.computeIfAbsent(route, r -> new RRNettyImpl<>(route, requestType, responseType));
        return rrRoute;
    }

    @Override
    public <REQUEST, RESPONSE> RRA<REQUEST, RESPONSE> createRRA(final String route, final Class<REQUEST> requestType, final Class<RESPONSE> responseType) {
        final RRANettyImpl<REQUEST, RESPONSE> rraRoute = this.rraRoutes.computeIfAbsent(route, r -> new RRANettyImpl<>(route, requestType, responseType));
        return rraRoute;
    }

    @Override
    public <SUBSCRIPTION, INITIAL_DATA, DATA> PS<SUBSCRIPTION, INITIAL_DATA, DATA> createPS(final String route, final Class<SUBSCRIPTION> subscriptionType, final Class<INITIAL_DATA> initialDataType, final Class<DATA> dataType) {
        final PSNettyImpl<SUBSCRIPTION, INITIAL_DATA, DATA> psRoute = this.psRoutes.computeIfAbsent(route, r -> new PSNettyImpl<>(route, subscriptionType, initialDataType, dataType));
        return psRoute;
    }

    @Override
    public <SUBSCRIPTION, INITIAL_DATA, DATA> SS<SUBSCRIPTION, INITIAL_DATA, DATA> createSS(final String route, final Class<SUBSCRIPTION> subscriptionType,
        final Class<INITIAL_DATA> initialDataType, final Class<DATA> dataType) {
        final SSNettyImpl<SUBSCRIPTION, INITIAL_DATA, DATA> ssRoute = this.ssRoutes.computeIfAbsent(route, r -> new SSNettyImpl(route, subscriptionType, initialDataType, dataType));
        return ssRoute;
    }

    @Override
    public <BROADCAST> BC<BROADCAST> createBC(final String route, final Class<BROADCAST> broadcastType) {
        return this.bcRoutes.computeIfAbsent(route, r -> new BCNettyImpl(route, broadcastType));
    }

    public void onRRResponse(final ApplicationMessage applicationMessage) {
        final RRNettyImpl rrRoute = rrRoutes.get(applicationMessage.getRoute());
        if (Objects.isNull(rrRoute)) {
            logger.warn("[RR] Unexpected ApplicationMessage={}", applicationMessage);
            return;
        }
        rrRoute.response(applicationMessage.getCorrelationId(), applicationMessage.getData());
    }

    public void onRRError(final ErrorMessage errorMessage) {
        final RRNettyImpl rrRoute = rrRoutes.get(errorMessage.getRoute());
        if (Objects.isNull(rrRoute)) {
            logger.warn("[RR] Unexpected ErrorMessage={}", errorMessage);
            return;
        }
        rrRoute.error(errorMessage);
    }

    public void onRRAResponse(final ApplicationMessage responseMsg) {
        final RRANettyImpl rraRoute = this.rraRoutes.get(responseMsg.getRoute());
        if (Objects.isNull(rraRoute)) {
            logger.warn("[RRA] Unexpected ApplicationMessage={}", responseMsg);
            return;
        }
        rraRoute.response(responseMsg.getCorrelationId(), responseMsg.getData());
    }

    public void onRRAError(final ErrorMessage errorMessage) {
        final RRANettyImpl rraRoute = this.rraRoutes.get(errorMessage.getRoute());
        if (Objects.isNull(rraRoute)) {
            logger.warn("[RRA] Unexpected ErrorMessage={}", errorMessage);
            return;
        }
        rraRoute.error(errorMessage);
    }

    public void onPSInitialData(final ApplicationMessage applicationMessage) {
        final PSNettyImpl psRoute = this.psRoutes.get(applicationMessage.getRoute());
        if (Objects.isNull(psRoute)) {
            logger.warn("[PS] Unexpected ApplicationMessage={}", applicationMessage);
            return;
        }
        psRoute.initialData(applicationMessage.getCorrelationId(), applicationMessage.getData());
    }

    public void onPSData(final ApplicationMessage applicationMessage) {
        final PSNettyImpl psRoute = this.psRoutes.get(applicationMessage.getRoute());
        if (Objects.isNull(psRoute)) {
            logger.warn("[PS] Unexpected ApplicationMessage={}", applicationMessage);
            return;
        }
        psRoute.data(applicationMessage.getCorrelationId(), applicationMessage.getData());
    }

    public void onSSInitialData(final ApplicationMessage applicationMessage) {
        final SSNettyImpl ssRoute = this.ssRoutes.get(applicationMessage.getRoute());
        if (Objects.isNull(ssRoute)) {
            logger.warn("[SS] Unexpected ApplicationMessage={}", applicationMessage);
            return;
        }
        ssRoute.initialData(applicationMessage.getCorrelationId(), applicationMessage.getData());
    }

    public void onSSData(final ApplicationMessage applicationMessage) {
        final SSNettyImpl ssRoute = this.ssRoutes.get(applicationMessage.getRoute());
        if (Objects.isNull(ssRoute)) {
            logger.warn("[SS] Unexpected ApplicationMessage={}", applicationMessage);
            return;
        }
        ssRoute.data(applicationMessage.getCorrelationId(), applicationMessage.getData());
    }

    public void onBroadcast(final ApplicationMessage applicationMessage) {
        final BCNettyImpl bcRoute = this.bcRoutes.get(applicationMessage.getRoute());
        if (Objects.isNull(bcRoute)) {
            logger.warn("[BC] Unexpected ApplicationMessage={}", applicationMessage);
            return;
        }
        bcRoute.onBroadcast(applicationMessage.getCorrelationId(), applicationMessage.getData());
    }

    @RequiredArgsConstructor
    class RRNettyImpl<REQUEST, RESPONSE> implements RR<REQUEST, RESPONSE> {

        private final String route;
        private final Class<REQUEST> requestType;
        private final Class<RESPONSE> responseType;

        private final Map<String, RequestAndPromiseRecord<REQUEST, RESPONSE>> rrPromises = new ConcurrentHashMap<>();

        @Override
        public void request(final REQUEST request, final RRPromise<REQUEST, RESPONSE> promise) {
            final String requestId = UUID.randomUUID().toString();
            logger.info("RR Service={}, Route={}, Request={}", serviceName, this.route, request);

            final byte[] bytes = serializer.<REQUEST>serialize(request, this.requestType);
            final ApplicationMessage requestMsg = new ApplicationMessage(requestId, MessageType.DATA, serviceName, route, MessagingPattern.RR, bytes);
            this.rrPromises.put(requestMsg.getCorrelationId(), new RequestAndPromiseRecord<>(request, promise));

            channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(requestMsg)));
        }

        @Override
        public void response(final String correlationId, final byte[] responseBytes) {
            final RequestAndPromiseRecord<REQUEST, RESPONSE> rrPromise = this.rrPromises.remove(correlationId);
            if (Objects.isNull(rrPromise)) {
                logger.warn("RR received unknown message for CorrelationId={}", correlationId);
                return;
            }
            final RESPONSE response = deserializer.deserialize(responseBytes, this.responseType);
            rrPromise.getPromise().onResponse(rrPromise.getRequest(), response);
        }

        @Override
        public void error(final ErrorMessage errorMessage) {
            final RequestAndPromiseRecord<REQUEST, RESPONSE> rrPromise = this.rrPromises.remove(errorMessage.getCorrelationId());
            if (Objects.isNull(rrPromise)) {
                logger.warn("RR received unknown message for CorrelationId={}", errorMessage.getCorrelationId());
                return;
            }
            rrPromise.getPromise().onError(errorMessage);
        }
    }

    @RequiredArgsConstructor
    class RRANettyImpl<REQUEST, RESPONSE> implements RRA<REQUEST, RESPONSE> {

        private final String route;
        private final Class<REQUEST> requestType;
        private final Class<RESPONSE> responseType;

        private final Map<String, RequestAndRRAPromiseRecord<REQUEST, RESPONSE>> rraPromises = new ConcurrentHashMap<>();

        @Override
        public void request(final REQUEST request, final RRAPromise<REQUEST, RESPONSE> promise) {
            final String correlationId = UUID.randomUUID().toString();

            logger.info("RRA Service={}, Route={}, Request={}", serviceName, this.route, request);
            final byte[] bytes = serializer.serialize(request, this.requestType);
            final ApplicationMessage applicationMessage = new ApplicationMessage(correlationId, MessageType.DATA, serviceName, this.route, MessagingPattern.RRA, bytes);
            this.rraPromises.put(correlationId, new RequestAndRRAPromiseRecord<>(request, promise));

            channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(applicationMessage)));
        }

        @Override
        public void response(final String correlationId, final byte[] responseBytes) {
            final RequestAndRRAPromiseRecord<REQUEST, RESPONSE> requestAndPromise = this.rraPromises.get(correlationId);
            if (Objects.isNull(requestAndPromise)) {
                logger.warn("Received response for unknown RRARequestId={}", correlationId);
                return;
            }
            final RESPONSE response = deserializer.deserialize(responseBytes, this.responseType);
            final Ack ack = ()-> {
                final RequestAndRRAPromiseRecord<REQUEST, RESPONSE> rraPromise = this.rraPromises.remove(correlationId);
                final byte[] bytes = serializer.serialize(requestAndPromise.getRequest(), this.requestType);
                final ApplicationMessage ackMsg = new ApplicationMessage(correlationId, MessageType.DATA_ACK, serviceName, this.route, MessagingPattern.RRA, bytes);
                channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(ackMsg)));
            };
            requestAndPromise.getPromise().onResponse(requestAndPromise.getRequest(), response, ack);
        }

        @Override
        public void error(final ErrorMessage errorMessage) {
            final RequestAndRRAPromiseRecord<REQUEST, RESPONSE> requestAndPromise = this.rraPromises.remove(errorMessage.getCorrelationId());
            if (Objects.isNull(requestAndPromise)) {
                logger.warn("Received error for unknown RRARequestId={}", errorMessage.getCorrelationId());
                return;
            }
            requestAndPromise.getPromise().onError(errorMessage);
        }
    }

    @RequiredArgsConstructor
    class PSNettyImpl<SUBSCRIPTION, INITIAL_DATA, DATA> implements PS<SUBSCRIPTION, INITIAL_DATA, DATA> {

        private final String route;
        private final Class<SUBSCRIPTION> subscriptionType;
        private final Class<INITIAL_DATA> initialDataType;
        private final Class<DATA> dataType;

        private final Map<String, SubscriptionAndStreamCallbackRecord<SUBSCRIPTION, INITIAL_DATA, DATA>> subscriptions = new ConcurrentHashMap<>();

        @Override
        public void subscribe(final SUBSCRIPTION subscription, final StreamCallback<SUBSCRIPTION, INITIAL_DATA, DATA> psCallback) {
            final String correlationId = UUID.randomUUID().toString();

            logger.info("PS Service={}, Route={}, Subscription={}", serviceName, this.route, subscription);
            final byte[] bytes = serializer.serialize(subscription, this.subscriptionType);
            final ApplicationMessage subscriptionMsg = new ApplicationMessage(correlationId, MessageType.DATA, serviceName, this.route, MessagingPattern.PS, bytes);
            this.subscriptions.put(correlationId, new SubscriptionAndStreamCallbackRecord<>(subscription, psCallback));

            channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(subscriptionMsg)));
        }

        @Override
        public void initialData(final String correlationId, final byte[] initialDataBytes) {
            final SubscriptionAndStreamCallbackRecord<SUBSCRIPTION, INITIAL_DATA, DATA> subscriptionAndCallback = this.subscriptions.get(correlationId);
            if (Objects.isNull(subscriptionAndCallback)) {
                logger.warn("Received initial data for unknown SubscriptionId={}", correlationId);
                return;
            }
            final INITIAL_DATA initialData = deserializer.deserialize(initialDataBytes, this.initialDataType);
            subscriptionAndCallback.getStreamCallback().onInitialData(initialData);
        }

        @Override
        public void data(final String correlationId, final byte[] dataBytes) {
            final SubscriptionAndStreamCallbackRecord<SUBSCRIPTION, INITIAL_DATA, DATA> subscriptionAndCallback = this.subscriptions.get(correlationId);
            if (Objects.isNull(subscriptionAndCallback)) {
                logger.warn("Received data for unknown SubscriptionId={}", correlationId);
                return;
            }
            final DATA data = deserializer.deserialize(dataBytes, this.dataType);
            subscriptionAndCallback.getStreamCallback().onData(data);
        }

        @Override
        public void close(final SUBSCRIPTION subscription) {
            final String correlationId = this.subscriptions.entrySet().stream().filter(e -> subscription.equals(e.getValue().getSubscription())).map(e -> e.getKey()).findFirst().orElse(null);
            if (Objects.isNull(correlationId)) {
                logger.warn("Could not find action Subscription={}", subscription);
                return;
            }
            final SubscriptionAndStreamCallbackRecord<SUBSCRIPTION, INITIAL_DATA, DATA> subscriptionAndCallback = this.subscriptions.remove(correlationId);
            final byte[] bytes = serializer.serialize(subscriptionAndCallback.getSubscription(), this.subscriptionType);
            final ApplicationMessage closeMsg = new ApplicationMessage(correlationId, MessageType.DATA_CLOSE, serviceName, this.route, MessagingPattern.PS, bytes);
            channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(closeMsg)));
        }
    }

    @RequiredArgsConstructor
    class SSNettyImpl<SUBSCRIPTION, INITIAL_DATA, DATA> implements SS<SUBSCRIPTION, INITIAL_DATA, DATA> {
        private final String route;
        private final Class<SUBSCRIPTION> subscriptionType;
        private final Class<INITIAL_DATA> initialDataType;
        private final Class<DATA> dataType;

        private final Map<String, SubscriptionAndStreamCallbackRecord<SUBSCRIPTION, INITIAL_DATA, DATA>> subscriptions = new ConcurrentHashMap<>();

        @Override
        public void subscribe(final SUBSCRIPTION subscription, final StreamCallback<SUBSCRIPTION, INITIAL_DATA, DATA> ssCallback) {
            final String correlationId = UUID.randomUUID().toString();

            logger.info("SS Service={}, Route={}, Subscription={}", serviceName, this.route, subscription);
            final byte[] bytes = serializer.serialize(subscription, this.subscriptionType);
            final ApplicationMessage subscriptionMsg = new ApplicationMessage(correlationId, MessageType.DATA, serviceName, this.route, MessagingPattern.SS, bytes);
            this.subscriptions.put(correlationId, new SubscriptionAndStreamCallbackRecord<>(subscription, ssCallback));

            channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(subscriptionMsg)));
        }

        @Override
        public void initialData(final String correlationId,final byte[] initialDataBytes) {
            final SubscriptionAndStreamCallbackRecord<SUBSCRIPTION, INITIAL_DATA, DATA> subscription = this.subscriptions.get(correlationId);
            if (Objects.isNull(subscription)) {
                logger.warn("Unexpected message for SubscriptionId={}", correlationId);
                return;
            }
            final INITIAL_DATA initialData = (INITIAL_DATA) deserializer.deserialize(initialDataBytes, this.initialDataType);
            subscription.getStreamCallback().onInitialData(initialData);
        }

        @Override
        public void data(final String correlationId,final byte[] dataBytes) {
            final SubscriptionAndStreamCallbackRecord<SUBSCRIPTION, INITIAL_DATA, DATA> subscription = this.subscriptions.get(correlationId);
            if (Objects.isNull(subscription)) {
                logger.warn("Unexpected message for SubscriptionId={}", correlationId);
                return;
            }
            final DATA data = deserializer.deserialize(dataBytes, dataType);
            subscription.getStreamCallback().onData(data);
        }

        @Override
        public void close(final SUBSCRIPTION subscription) {
            final String correlationId = this.subscriptions.entrySet().stream().filter(e -> subscription.equals(e.getValue().getSubscription())).map(e -> e.getKey()).findFirst().orElse(null);
            if (Objects.isNull(correlationId)) {
                logger.warn("Could not find Subscription={}", subscription);
                return;
            }
            final SubscriptionAndStreamCallbackRecord<SUBSCRIPTION, INITIAL_DATA, DATA> subscriptionAndCallback = this.subscriptions.remove(correlationId);
            final byte[] bytes = serializer.serialize(subscriptionAndCallback.getSubscription(), this.subscriptionType);
            final ApplicationMessage closeMsg = new ApplicationMessage(correlationId, MessageType.DATA_CLOSE, serviceName, this.route, MessagingPattern.SS, bytes);
            channelCtx.writeAndFlush(new TextWebSocketFrame(NETTY_JSON_CODEC.serializeToString(closeMsg)));
        }
    }

    @RequiredArgsConstructor
    class BCNettyImpl<BROADCAST> implements BC<BROADCAST> {

        private final String route;
        private final Class<BROADCAST> broadcastType;

        private final Set<Consumer<BROADCAST>> broadcastListeners = new CopyOnWriteArraySet<>();

        @Override
        public void joinBroadcast(final Consumer<BROADCAST> onBroadcastMessage) {
            this.broadcastListeners.add(onBroadcastMessage);
        }

        @Override
        public void onBroadcast(final String correlationId, final byte[] broadcastBytes) {
            final BROADCAST broadcast = deserializer.deserialize(broadcastBytes, this.broadcastType);

            for (final Consumer<BROADCAST> broadcastConsumer : this.broadcastListeners) {
                try {
                    broadcastConsumer.accept(broadcast);
                } catch (final Exception e) {
                    logger.error("Failed to process Broadcast={}", broadcast);
                }
            }
        }
    }
}
