package com.nikoskatsanos.bayleaf.netty.example;

import com.nikoskatsanos.bayleaf.core.ConnectorRegistry;
import com.nikoskatsanos.bayleaf.core.util.BayLeafThreadFactory;
import com.nikoskatsanos.bayleaf.netty.BayLeafServer;
import com.nikoskatsanos.bayleaf.netty.BayLeafServer.Builder;
import com.nikoskatsanos.bayleaf.core.auth.NullAuthenticator;
import com.nikoskatsanos.bayleaf.core.auth.NullAuthorizer;
import com.nikoskatsanos.bayleaf.netty.dispatch.DispatchingStrategy.OrderedDispatcher;
import com.nikoskatsanos.bayleaf.netty.example.connector.TradeConnector;
import com.nikoskatsanos.bayleaf.web.BayLeafStaticContentServer;
import java.io.File;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyEchoServer {

    public static void main(String[] args) {

        BayLeafStaticContentServer staticContentServer = BayLeafStaticContentServer
            .newBuilder()
            .withPort(9997)
            .withContext("")
            .withResourcePath("./bay-leaf-example/src/main/resources/public")
            .withSecurePort(9998)
            .withKeystore("./bay-leaf-example/src/main/resources/BayLeafTestKeyPair", "changeit")
            .build();
        staticContentServer.start();

        final ConnectorRegistry connectorRegistry = new ConnectorRegistry();
        connectorRegistry.registerConnector(new EchoConnector("echoService"));
        connectorRegistry.registerConnector(new MarketDataConnector("marketData"));
        connectorRegistry.registerConnector(new TradeConnector("tradeService"));

        final File cert = new File("bay-leaf-example/src/main/resources/test.cer");
        final File keystore = new File("bay-leaf-example/src/main/resources/test_key.pem");
        final File jksKeystore = new File("bay-leaf-example/src/main/resources/BayLeafTestKeyPair.jks");

        final BayLeafServer server = new Builder()
            .withPort(9999)
            .withAuthenticator(new NullAuthenticator())
            .withAuthorizer(new NullAuthorizer())
            .withConnectorRegistry(connectorRegistry)
//            .withSSLContext(cert, keystore, null)
            .withSSLContext(jksKeystore, "changeit".toCharArray())
            .withDispatchingStrategy(new OrderedDispatcher(Executors.newFixedThreadPool(8, new BayLeafThreadFactory("EchoServiceDispatcher"))))
            .build();
        server.start();
    }
}
