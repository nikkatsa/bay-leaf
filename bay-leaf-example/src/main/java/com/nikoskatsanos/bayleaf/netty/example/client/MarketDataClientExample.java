package com.nikoskatsanos.bayleaf.netty.example.client;

import com.nikoskatsanos.bayleaf.client.SessionCallback.NullSessionCallback;
import com.nikoskatsanos.bayleaf.client.auth.ClientAuthTokenProvider.UsernamePasswordClientAuthTokenProvider;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafClient;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafService.RR;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafService.SS;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafServiceNettyImpl;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.RRPromise;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.StreamCallback;
import com.nikoskatsanos.bayleaf.core.codec.JsonCodec;
import com.nikoskatsanos.bayleaf.core.message.ErrorMessage;
import com.nikoskatsanos.bayleaf.netty.example.model.MarketDataRequest;
import com.nikoskatsanos.bayleaf.netty.example.model.MarketDataResponse;
import com.nikoskatsanos.bayleaf.netty.example.model.SecurityListRequest;
import com.nikoskatsanos.bayleaf.netty.example.model.SecurityListResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarketDataClientExample {

    public static void main(String[] args) throws Exception {
        final BayLeafClient client = BayLeafClient.builder()
            .withHost("localhost")
            .withPort(9999)
            .withSSLTrustStore(new File("bay-leaf-example/src/main/resources/BayLeafTestCertTruststore.jks"), "changeit".toCharArray())
            .withAuthTokenProvider(new UsernamePasswordClientAuthTokenProvider("test", "test"))
            .build();
        client.start(new NullSessionCallback()).get(5_000, TimeUnit.MILLISECONDS);

        final JsonCodec jsonCodec = new JsonCodec();
        final BayLeafServiceNettyImpl marketDataService = client.createService("marketData", jsonCodec.serializer(), jsonCodec.deserializer());

        final RR<SecurityListRequest, SecurityListResponse> securityList = marketDataService.createRR("securityList", SecurityListRequest.class, SecurityListResponse.class);

        final List<String> ccys = new ArrayList<>();
        final CountDownLatch securityListReceived = new CountDownLatch(1);
        securityList.request(new SecurityListRequest(), new RRPromise<SecurityListRequest, SecurityListResponse>() {
            @Override
            public void onResponse(final SecurityListRequest securityListRequest, final SecurityListResponse securityListResponse) {
                logger.info("SecurityList={}", securityListResponse);
                ccys.addAll(securityListResponse.getCcyList());
                securityListReceived.countDown();
            }

            @Override
            public void onError(final ErrorMessage errorMsg) {
                logger.error(errorMsg.toString());
                securityListReceived.countDown();
            }
        });

        securityListReceived.await(1_000, TimeUnit.MILLISECONDS);

        final StreamCallback<MarketDataRequest, MarketDataResponse, MarketDataResponse> marketDataCallback = new StreamCallback<>() {
            @Override
            public void onInitialData(final MarketDataResponse marketDataResponse) {
                logger.info("[SNAPSHOT] Ccy={}, MarketData={}", marketDataResponse.getSymbol(), marketDataResponse);
            }

            @Override
            public void onData(final MarketDataResponse marketDataResponse) {
                logger.info("[DATA] Ccy={}, MarketData={}",marketDataResponse.getSymbol(), marketDataResponse);
            }
        };
        final SS<MarketDataRequest, MarketDataResponse, MarketDataResponse> marketData = marketDataService.createSS("stream", MarketDataRequest.class, MarketDataResponse.class, MarketDataResponse.class);
        for (final String ccy : ccys) {
            marketData.subscribe(new MarketDataRequest(ccy), marketDataCallback);
        }

        TimeUnit.SECONDS.sleep(30);
        client.close();
    }
}
