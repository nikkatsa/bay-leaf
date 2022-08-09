package com.nikoskatsanos.bayleaf.netty.example.client;

import com.nikoskatsanos.bayleaf.client.SessionCallback.NullSessionCallback;
import com.nikoskatsanos.bayleaf.client.auth.ClientAuthTokenProvider.UsernamePasswordClientAuthTokenProvider;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafClient;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafService;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafService.BC;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafService.PS;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafService.RR;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafService.RRA;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafService.SS;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafServiceNettyImpl;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.StreamCallback;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.RRAPromise;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.RRPromise;
import com.nikoskatsanos.bayleaf.core.codec.StringCodec;
import com.nikoskatsanos.bayleaf.domain.message.ErrorMessage;
import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EchoServerClientExample {

    public static void main(String[] args) {
        try {
            final BayLeafClient client = BayLeafClient.builder()
                .withHost("localhost")
                .withPort(9999)
                .withSSLTrustStore(new File("bay-leaf-example/src/main/resources/BayLeafTestCertTruststore.jks"), "changeit".toCharArray())
                .withAuthTokenProvider(new UsernamePasswordClientAuthTokenProvider("test", "test"))
                .build();
            final Future<Void> sessionInitializedFuture = client.start(new NullSessionCallback());
            sessionInitializedFuture.get(5_000, TimeUnit.MILLISECONDS);

            final StringCodec stringCodec = new StringCodec();
            final BayLeafServiceNettyImpl echoService = client.createService("echoService", stringCodec.serializer(), stringCodec.deserializer());

            testRR(echoService);

            testRRA(echoService);

            testPS(echoService);

            testSS(echoService);

            testBC(echoService);

            TimeUnit.SECONDS.sleep(30);
            client.close();
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static void testBC(BayLeafServiceNettyImpl echoService) {
        final BC<String> echoBroadcast = echoService.createBC("echoBroadcast", String.class);
        echoBroadcast.joinBroadcast(bc -> logger.info("[BC] EchoBroadcast={}", bc));
    }

    private static void testSS(BayLeafServiceNettyImpl echoService) {
        final SS<String, String, String> sharedStream = echoService.createSS("echoShared", String.class, String.class, String.class);
        sharedStream.subscribe("SharedStreamSubscription", new StreamCallback<String, String, String>() {
            @Override
            public void onInitialData(final String initialData) {
                logger.info("[SS] Subscription=SharedSubscription, InitialData={}", initialData);
            }

            @Override
            public void onData(final String data) {
                logger.info("[SS] Subscription=SharedSubscription, Data={}", data);
            }
        });
    }

    private static void testPS(BayLeafServiceNettyImpl echoService) {
        final PS<String, String, String> echoPS = echoService.createPS("echoStream", String.class, String.class, String.class);
        echoPS.subscribe("PSSub", new StreamCallback<String, String, String>() {
            volatile int counter = 0;

            @Override
            public void onInitialData(final String initialData) {
                logger.info("[PS] Subscription=PSSub, InitialData={}", initialData);
            }

            @Override
            public void onData(final String data) {
                logger.info("[PS] Subscription=PSSub, Data={}", data);
                if (++counter >= 5) {
                    logger.warn("Closing PS Subscription=PSSUb");
                    echoPS.close("PSSub");
                }
            }
        });
    }

    private static void testRRA(BayLeafServiceNettyImpl echoService) {
        final RRA<String, String> echoRRA = echoService.createRRA("echoAck", String.class, String.class);
        echoRRA.request("HelloRRA", new RRAPromise<String, String>() {
            @Override
            public void onResponse(final String request, final String response, final Ack ack) {
                logger.info("RRA Request={}, Response={}", request, response);
                ack.ack();
            }

            @Override
            public void onError(final ErrorMessage errorMessage) {
                logger.error("Request=HelloRRA, Error={}", errorMessage);
            }
        });
    }

    private static void testRR(final BayLeafService echoService) {
        final RR<String, String> echoRR = echoService.createRR("echo", String.class, String.class);
        echoRR.request("Hello", new RRPromise<String, String>() {
            @Override
            public void onResponse(String request, String response) {
                logger.info("Response={}", response);
            }

            @Override
            public void onError(final ErrorMessage errorMsg) {
                logger.error("Request={}, Error={}", "Hello", errorMsg);
            }
        });
    }
}
