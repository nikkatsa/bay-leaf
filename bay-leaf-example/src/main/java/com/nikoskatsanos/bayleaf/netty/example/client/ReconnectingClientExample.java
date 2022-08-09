package com.nikoskatsanos.bayleaf.netty.example.client;

import com.nikoskatsanos.bayleaf.client.SessionCallback;
import com.nikoskatsanos.bayleaf.client.SessionCallback.NullSessionCallback;
import com.nikoskatsanos.bayleaf.client.auth.ClientAuthTokenProvider.UsernamePasswordClientAuthTokenProvider;
import com.nikoskatsanos.bayleaf.client.netty.BayLeafClient;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReconnectingClientExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {
        final BayLeafClient client = BayLeafClient.builder()
            .withHost("localhost")
            .withPort(9999)
            .withSSLTrustStore(new File("bay-leaf-example/src/main/resources/BayLeafTestCertTruststore.jks"), "changeit".toCharArray())
            .withAuthTokenProvider(new UsernamePasswordClientAuthTokenProvider("test", "test"))
            .build();
        final Future<Void> sessionInitializedFuture = client.start(new LoggingSessionCallback());

        Thread.currentThread().join();
    }

    static class LoggingSessionCallback implements SessionCallback {

        @Override
        public void onSessionInitialized() {
            logger.info("SessionInitialized");
        }

        @Override
        public void onSessionDestroyed() {
            logger.info("SessionDestroyed");
        }
    }
}
