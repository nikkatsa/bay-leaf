package com.nikoskatsanos.bayleaf.netty.example;

import com.nikoskatsanos.bayleaf.core.ConnectorRegistry;
import com.nikoskatsanos.bayleaf.netty.BayLeafServer;
import com.nikoskatsanos.bayleaf.netty.BayLeafServer.Builder;
import com.nikoskatsanos.bayleaf.core.auth.NullAuthenticator;
import com.nikoskatsanos.bayleaf.core.auth.NullAuthorizer;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyEchoServer {

    public static void main(String[] args) {
        final ConnectorRegistry connectorRegistry = new ConnectorRegistry();
        connectorRegistry.registerConnector(new EchoConnector("echoService"));

        final File cert = new File("bay-leaf-example/src/main/resources/test.cer");
        final File keystore = new File("bay-leaf-example/src/main/resources/test_key.pem");

        final BayLeafServer server = new Builder()
            .withPort(9999)
            .withAuthenticator(new NullAuthenticator())
            .withAuthorizer(new NullAuthorizer())
            .withConnectorRegistry(connectorRegistry)
            .withSSLContext(cert, keystore, null)
            .build();
        server.start();
    }
}
