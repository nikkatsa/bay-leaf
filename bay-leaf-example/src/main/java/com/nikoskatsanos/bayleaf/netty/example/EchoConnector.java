package com.nikoskatsanos.bayleaf.netty.example;

import com.nikoskatsanos.bayleaf.core.codec.StringCodec;
import com.nikoskatsanos.bayleaf.server.Connector;
import com.nikoskatsanos.bayleaf.server.messagingpattern.BC;
import com.nikoskatsanos.bayleaf.server.messagingpattern.BCContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.Broadcast;
import com.nikoskatsanos.bayleaf.server.messagingpattern.PS;
import com.nikoskatsanos.bayleaf.server.messagingpattern.PSContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RR;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RRA;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RRAContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RRContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.Request;
import com.nikoskatsanos.bayleaf.server.messagingpattern.Response;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SS;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SSContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SharedSubscription;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SharedSubscriptionData;
import com.nikoskatsanos.bayleaf.server.messagingpattern.Subscription;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SubscriptionData;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EchoConnector extends Connector {

    private static final StringCodec STRING_CODEC = new StringCodec();

    private BCContext<String> echoBroadcast;
    private ScheduledExecutorService broadcaster;

    private final Map<String, EchoStreamer> echoStreamers = new HashMap<>();
    private final ScheduledExecutorService echoStreamerRepeater = Executors.newScheduledThreadPool(1);

    private final Map<String, EchoSharedStreamer> sharedStreamers = new HashMap<>();

    public EchoConnector(String name) {
        super(name, STRING_CODEC.deserializer(), STRING_CODEC.serializer());
    }

    @RR(name = "echo", requestType = String.class, responseType = String.class)
    public void echoRR(final RRContext<String, String> rrContext) {
        rrContext.onRequest(request -> {
            logger.info("Endpoint=echo, Request={}, Session={}", request, rrContext.session());
            final EchoResponseSender echoResponseSender = new EchoResponseSender(request, rrContext);
            echoResponseSender.echo(request.getRequest() + "back");
        });
    }

    @RR(name = "echoError", requestType = String.class, responseType = String.class)
    public void echoError(final RRContext<String, String> rrContext) {
        rrContext.onRequest(request -> {
            logger.info("Endpoint=echoError, Request={}, Session={}", request, rrContext.session());
            final EchoResponseSender echoResponseSender = new EchoResponseSender(request, rrContext);
            echoResponseSender.err();
        });
    }

    @RRA(name = "echoAck", requestType = String.class, responseType = String.class, ackTimeout = 1)
    public void echoAck(final RRAContext<String, String> rraContext) {

        final EchoResponseAckSender responseAckSender = new EchoResponseAckSender(rraContext);
        rraContext.onRequest(request -> {
            logger.info("Endpoint=echoAck, Request={}, Session={}", request, rraContext.session());
            responseAckSender.setRequest(request);
            responseAckSender.echo(String.format("%s. Awaiting ack", request.getRequest()));
        });
        rraContext.onAck(v -> {
            logger.info("Ack received");
        });
        rraContext.onAckTimeout(v -> logger.error("Timed out"));
    }

    @BC(name = "echoBroadcast", broadcastType = String.class)
    public void broadcast(final BCContext<String> broadcastContext) {
        logger.info("BroadcastEndpoint=echoBroadcast enabled", broadcastContext);
        this.echoBroadcast = broadcastContext;

        synchronized (this) {
            if (Objects.nonNull(this.broadcaster)) {
                return;
            }
            this.broadcaster = Executors.newScheduledThreadPool(1);
            broadcaster.scheduleAtFixedRate(() -> {
                if (Objects.nonNull(this.echoBroadcast)) {
                    this.echoBroadcast.broadcast(new Broadcast<>("Current server time " + Instant.ofEpochMilli(System.currentTimeMillis())));
                }
            }, 5_000, 5_000, TimeUnit.MILLISECONDS);
        }
    }

    @PS(name = "echoStream", subscriptionType = String.class, initialDataType = String.class, dataType = String.class)
    public void privateStream(final PSContext<String, String, String> psContext) {
        psContext.onSubscription(sub -> {
            logger.info("Subscription Request={}, Session={}", sub, psContext.session());
            final EchoStreamer echoStreamer = new EchoStreamer(sub, psContext);
            this.echoStreamers.put(sub.getId(), echoStreamer);

            echoStreamer.initData();
            echoStreamer.start();
        });
        psContext.onClose(sub -> {
            final EchoStreamer echoStreamer = this.echoStreamers.remove(sub.getId());
            if (Objects.nonNull(echoStreamer)) {
                echoStreamer.stop();
            }
        });
    }

    @PS(name="echoStreamServerClose", subscriptionType = String.class, initialDataType = String.class, dataType = String.class)
    public void echoStreamServerClose(final PSContext<String, String, String> psContext) {
        psContext.onSubscription(sub -> {
            logger.info("Subscription Route=echoStreamServerClose, Subscription={}, Session={}", sub, psContext.session());
            psContext.data(new SubscriptionData<>(sub, "Server will be closing stream in 3secs"));

            Executors.newScheduledThreadPool(0).schedule(() -> {
                psContext.close(new SubscriptionData<>(sub, null));
            }, 3_000, TimeUnit.MILLISECONDS);
        });
    }

    @SS(name = "echoShared", subscriptionType = String.class, initialDataType = String.class, dataType = String.class)
    public void sharedStream(final SSContext<String, String, String> ssContext) {
        ssContext.onSubscription(ssSub -> {
            final EchoSharedStreamer echoSharedStreamer = this.sharedStreamers
                .computeIfAbsent(ssSub.getSubscription(), k -> new EchoSharedStreamer(ssSub, ssContext.streamContext(ssSub.getSubscription())));
            echoSharedStreamer.increaseSubscribers();
        });
        ssContext.onSubscriptionClose(ssSub -> {
            final EchoSharedStreamer echoSharedStreamer = this.sharedStreamers.get(ssSub.getSubscription());
            if (Objects.nonNull(echoSharedStreamer)) {
                echoSharedStreamer.decreaseSubscribers();
            }
        });
    }

    @Override
    public void stop() {
        super.stop();
        if (Objects.nonNull(this.broadcaster)) {
            this.broadcaster.shutdownNow();
        }
        if (Objects.nonNull(this.echoStreamerRepeater)) {
            this.echoStreamerRepeater.shutdownNow();
        }
    }

    @RequiredArgsConstructor
    private class EchoResponseSender {

        private final Request<String> request;
        private final RRContext<String, String> context;

        public void echo(final String response) {
            context.response(new Response<String, String>(response, this.request));
        }

        public void err() {
            this.context.error(1, "Failed to process " + this.request.getRequest(), new Response<>(null, this.request));
        }
    }

    @RequiredArgsConstructor
    private class EchoResponseAckSender {

        @Setter
        private Request<String> request;
        private final RRAContext<String, String> context;

        public void echo(final String response) {
            context.response(new Response<>(response, this.request));
        }
    }

    @RequiredArgsConstructor
    @AllArgsConstructor
    private class EchoStreamer {

        @Setter
        private Subscription<String> subscription;
        private final PSContext<String, String, String> context;
        private long counter = 0;
        private ScheduledFuture<?> streamingTask;

        public EchoStreamer(Subscription<String> subscription, PSContext<String, String, String> context) {
            this.subscription = subscription;
            this.context = context;
        }

        void initData() {
            this.context.initialData(new SubscriptionData<>(this.subscription, "Hello. Starting"));
        }

        void data(final String data) {
            this.context.data(new SubscriptionData<>(this.subscription, data));
        }

        void start() {
            logger.info("Starting echo streamer for Session={}", this.context.session());
            this.streamingTask = echoStreamerRepeater.scheduleAtFixedRate(() -> {
                try {
                    this.data(String.valueOf(counter++));
                } catch (final Exception e) {
                    logger.error("Failed to stream data to Session={}", this.context.session(), e);
                }
            }, 0, 5_000, TimeUnit.MILLISECONDS);
        }

        void stop() {
            logger.info("Stopping echo streamer for Session={}", this.context.session());
            if (Objects.nonNull(this.streamingTask)) {
                this.streamingTask.cancel(true);
            }
        }
    }

    @RequiredArgsConstructor
    class EchoSharedStreamer {

        private final SharedSubscription<String> subscription;
        private final SSContext.StreamContext<String, String> streamContext;

        private final AtomicInteger subscribers = new AtomicInteger(0);
        private int counter = 0;
        private final ScheduledExecutorService sharedStreamThread = Executors.newScheduledThreadPool(1);
        private ScheduledFuture<?> streamingTask;

        synchronized void start() {
            logger.info("Starting stream for {}", this.subscription);
            this.streamContext.onClose(this::onSharedStreamClosed);

            this.streamingTask = this.sharedStreamThread.scheduleAtFixedRate(() -> {
                this.streamContext.stream(new SharedSubscriptionData<>(this.subscription, String.format("%s-%s", this.subscription.getSubscription(), counter++)));
            }, 0L, 3_000L, TimeUnit.MILLISECONDS);
        }

        synchronized void stop() {
            logger.info("Stopping stream for {}", this.subscription);
            if (this.streamingTask != null) {
                this.streamingTask.cancel(true);
            }
        }

        void increaseSubscribers() {
            if (this.subscribers.incrementAndGet() == 1) {
                this.start();
            }
        }

        void decreaseSubscribers() {
            if (this.subscribers.decrementAndGet() == 0) {
                this.stop();
            }
        }

        void onSharedStreamClosed(final Void v) {
            this.stop();
            sharedStreamers.remove(this.subscription.getSubscription());
        }
    }
}
