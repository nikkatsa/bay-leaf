package com.nikoskatsanos.bayleaf.netty.example;

import com.nikoskatsanos.bayleaf.core.Connector;
import com.nikoskatsanos.bayleaf.core.codec.JsonCodec;
import com.nikoskatsanos.bayleaf.core.messagingpattern.RRA;
import com.nikoskatsanos.bayleaf.core.messagingpattern.RRAContext;
import com.nikoskatsanos.bayleaf.core.messagingpattern.Request;
import com.nikoskatsanos.bayleaf.core.messagingpattern.Response;
import com.nikoskatsanos.bayleaf.netty.example.model.TradeRequest;
import com.nikoskatsanos.bayleaf.netty.example.model.TradeResponse;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
        });
        context.onAckTimeout(noAck -> {
            logger.error("Ack timeout for Request={}", noAck);
        });
    }

    class TradeServer {

        private final AtomicInteger tradesCounter = new AtomicInteger();

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
    }
}
