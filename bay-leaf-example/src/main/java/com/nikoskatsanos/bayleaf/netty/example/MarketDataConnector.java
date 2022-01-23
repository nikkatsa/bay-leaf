package com.nikoskatsanos.bayleaf.netty.example;

import com.nikoskatsanos.bayleaf.core.codec.JsonCodec;
import com.nikoskatsanos.bayleaf.server.Connector;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RR;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RRContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.Response;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SS;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SSContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SSContext.StreamContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SharedSubscription;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SharedSubscriptionData;
import com.nikoskatsanos.bayleaf.core.util.BayLeafThreadFactory;
import com.nikoskatsanos.bayleaf.netty.example.model.MarketDataRequest;
import com.nikoskatsanos.bayleaf.netty.example.model.MarketDataResponse;
import com.nikoskatsanos.bayleaf.netty.example.model.SecurityListRequest;
import com.nikoskatsanos.bayleaf.netty.example.model.SecurityListResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarketDataConnector extends Connector {

    private static final JsonCodec JSON_CODEC = new JsonCodec();

    private static final List<String> CCY_LIST = Collections.unmodifiableList(Arrays.asList("EURUSD", "GBPUSD", "GBPEUR", "USDJPY", "USDAUD", "EURCFH"));

    private final Map<String, MarketDataGenerator> streamers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService randomStreams = Executors.newScheduledThreadPool(2, new BayLeafThreadFactory("MarketDataStreamer"));

    public MarketDataConnector(String name) {
        super(name, JSON_CODEC.deserializer(), JSON_CODEC.serializer());
    }

    @RR(name="securityList", requestType = SecurityListRequest.class, responseType = SecurityListResponse.class)
    public void securityList(final RRContext<SecurityListRequest, SecurityListResponse> rrContext) {
        rrContext.onRequest(req -> {
            rrContext.response(new Response<>(new SecurityListResponse(CCY_LIST), req));
        });
    }

    @SS(name = "stream", subscriptionType = MarketDataRequest.class, initialDataType = MarketDataResponse.class, dataType = MarketDataResponse.class)
    public void marketData(final SSContext<MarketDataRequest, MarketDataResponse, MarketDataResponse> ssContext) {
        ssContext.onSubscription(sub -> {
            logger.info("Received MarketDataRequest={}, Session={}", sub.getSubscription(), ssContext.session());

            final MarketDataGenerator marketDataGenerator = this.streamers
                .computeIfAbsent(sub.getSubscription().getSymbol(), k -> new MarketDataGenerator(sub, ssContext.streamContext(sub.getSubscription())));
            marketDataGenerator.start();
        });
        ssContext.onSubscriptionClose(sub -> {
            logger.info("Received MarketDataUnsubRequest={}, Session={}", sub.getSubscription(), ssContext.session());
            final MarketDataGenerator marketDataGenerator = this.streamers.get(sub.getSubscription().getSymbol());
            if (Objects.nonNull(marketDataGenerator)) {
                marketDataGenerator.stop();
            }
        });
    }

    @RequiredArgsConstructor
    class MarketDataGenerator {

        private boolean started = false;
        private int subscribers = 0;

        private final SharedSubscription<MarketDataRequest> marketDataRequest;
        private final StreamContext<MarketDataRequest, MarketDataResponse> streamContext;

        private double lastPrice = ThreadLocalRandom.current().nextDouble(1.1, 1.5);
        private ScheduledFuture<?> periodicPublisher;

        synchronized void start() {
            if (!this.started) {
                this.publishPrice();
                this.started = true;
            }
            this.subscribers++;
        }

        synchronized void stop() {
            this.subscribers--;
            if (this.subscribers == 0 && Objects.nonNull(this.periodicPublisher)) {
                this.periodicPublisher.cancel(false);
                this.started = false;
            }
        }

        void publishPrice() {
            this.lastPrice = this.lastPrice + (ThreadLocalRandom.current().nextInt() % 2 == 0 ? 1 : -1) * ThreadLocalRandom.current().nextInt(1, 5) * 0.01;

            final double bid = this.lastPrice - (this.lastPrice  * ThreadLocalRandom.current().nextDouble(0.01, 0.10));
            final double ask = this.lastPrice + (this.lastPrice  * ThreadLocalRandom.current().nextDouble(0.01, 0.10));
            this.streamContext
                .stream(new SharedSubscriptionData<>(this.marketDataRequest, new MarketDataResponse(this.marketDataRequest.getSubscription().getSymbol(), bid, ask)));

            this.periodicPublisher = randomStreams.schedule(this::publishPrice, ThreadLocalRandom.current().nextLong(0, 5000L), TimeUnit.MILLISECONDS);
        }
    }
}
