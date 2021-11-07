package com.nikoskatsanos.bayleaf.netty.example.connector;

import com.nikoskatsanos.bayleaf.core.Connector;
import com.nikoskatsanos.bayleaf.core.codec.JsonCodec;
import com.nikoskatsanos.bayleaf.core.messagingpattern.PS;
import com.nikoskatsanos.bayleaf.core.messagingpattern.PSContext;
import com.nikoskatsanos.bayleaf.core.messagingpattern.RRA;
import com.nikoskatsanos.bayleaf.core.messagingpattern.RRAContext;
import com.nikoskatsanos.bayleaf.core.messagingpattern.Request;
import com.nikoskatsanos.bayleaf.core.messagingpattern.Response;
import com.nikoskatsanos.bayleaf.core.messagingpattern.Subscription;
import com.nikoskatsanos.bayleaf.core.messagingpattern.SubscriptionData;
import com.nikoskatsanos.bayleaf.netty.example.model.Trade;
import com.nikoskatsanos.bayleaf.netty.example.model.TradeBlotterRequest;
import com.nikoskatsanos.bayleaf.netty.example.model.TradeBlotterResponse;
import com.nikoskatsanos.bayleaf.netty.example.model.TradeRequest;
import com.nikoskatsanos.bayleaf.netty.example.model.TradeResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeConnector extends Connector {

    private static final JsonCodec JSON_CODEC = new JsonCodec();

    private final TradeServer tradeServer = new TradeServer();

    public TradeConnector(String name) {
        super(name, JSON_CODEC.deserializer(), JSON_CODEC.serializer());
    }

    @RRA(name = "trade", requestType = TradeRequest.class, responseType = TradeResponse.class)
    public void trade(final RRAContext<TradeRequest, TradeResponse> context) {
        logger.info("Trade endpoint initialized for Session={}", context.session());
        context.onRequest(req -> {
            logger.info("TradeRequest={}, Session={}", req, context.session());
            this.tradeServer.trade(req, context);
        });
        context.onAck(ack -> {
            logger.info("Client ack received for Request={}", ack);
            this.tradeServer.tradeDone(ack, context);
        });
        context.onAckTimeout(noAck -> {
            logger.error("Ack timeout for Request={}", noAck);
        });
    }

    @PS(name = "blotter", subscriptionType = TradeBlotterRequest.class, dataType = TradeBlotterResponse.class)
    public void blotter(final PSContext<TradeBlotterRequest, TradeBlotterResponse> context) {
        context.onSubscription(sub -> {
            logger.info("Subscribing for TradeBlotterRequest={}, Session={}", sub, context.session());
            this.tradeServer.streamTrades(sub, context);
        });
        context.onClose(sub -> {
            logger.info("UnSubscribing for TradeBlotterRequest={}, Session={}", sub, context.session());
            this.tradeServer.stopStreamTrades(sub, context);
        });
    }

    class TradeServer {

        private final AtomicInteger tradesCounter = new AtomicInteger();

        private final Map<String, List<Trade>> tradesByUser = new ConcurrentHashMap<>();
        private final Map<String, SubscriptionAndContext> blotterStreams = new ConcurrentHashMap<>();

        void trade(final Request<TradeRequest> tradeRequest, final RRAContext<TradeRequest, TradeResponse> context) {
            final boolean shouldReject = this.tradesCounter.incrementAndGet() % 5 == 0;

            try {
                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(200, 1_000));
            } catch (final InterruptedException e) {
            }

            if (shouldReject) {
                context.error(1, "Trade rejected", new Response<>(new TradeResponse(tradeRequest.getRequest().getId(), !shouldReject, "Randomly rejected"), tradeRequest));
            } else {
                context.response(new Response<>(new TradeResponse(tradeRequest.getRequest().getId(), !shouldReject, null), tradeRequest));
            }
        }

        void tradeDone(final Request<TradeRequest> tradeRequest, final RRAContext<TradeRequest, TradeResponse> context) {
            final List<Trade> trades = this.tradesByUser.computeIfAbsent(context.session().getUser().getUserId(), k -> new ArrayList<>());
            final TradeRequest request = tradeRequest.getRequest();
            final Trade trade = new Trade(System.currentTimeMillis(), request.getId(), request.getSymbol(), request.getQuantity(), request.getPrice(), request.getSide());
            trades.add(trade);

            final SubscriptionAndContext blotterContext = this.blotterStreams.get(context.session().getUser().getUserId());
            if (Objects.nonNull(blotterContext)) {
                blotterContext.context.data(new SubscriptionData<>(blotterContext.subscription, new TradeBlotterResponse(Collections.singletonList(trade))));
            }
        }

        void streamTrades(final Subscription<TradeBlotterRequest> sub, final PSContext<TradeBlotterRequest, TradeBlotterResponse> context) {
            this.blotterStreams.putIfAbsent(context.session().getUser().getUserId(), new SubscriptionAndContext(sub, context));
            final List<Trade> trades = Optional.ofNullable(this.tradesByUser.get(context.session().getUser().getUserId())).orElse(Collections.emptyList());
            context.initialData(new SubscriptionData<>(sub, new TradeBlotterResponse(trades)));
        }

        void stopStreamTrades(final Subscription<TradeBlotterRequest> sub, final PSContext<TradeBlotterRequest, TradeBlotterResponse> context) {
            this.blotterStreams.remove(context.session().getUser().getUserId());
        }
    }

    @Data
    @AllArgsConstructor
    class SubscriptionAndContext {
        private final Subscription<TradeBlotterRequest> subscription;
        private final PSContext<TradeBlotterRequest, TradeBlotterResponse> context;
    }
}
