package com.nikoskatsanos.bayleaf.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

@RequiredArgsConstructor
@Slf4j
public class BayLeafStaticContentServer {

    private final Server server;

    public void start() {
        try {
            this.server.start();
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private int port = -1;
        private int securePort = -1;
        private String context = "";

        private Path keystore;
        private String keystorePass;

        private Path resourcePath;

        public Builder withPort(final int port) {
            this.port = port;
            return this;
        }

        public Builder withSecurePort(final int securePort) {
            this.securePort = securePort;
            return this;
        }

        public Builder withContext(final String context) {
            this.context = context;
            return this;
        }

        public Builder withKeystore(final String keystore, final String keystorePass) {
            this.keystore = Paths.get(keystore);
            this.keystorePass = keystorePass;
            if (Files.notExists(this.keystore)) {
                final String msg = String.format("Provided Keystore=%s does not exist", this.keystore);
                logger.error(msg);
                throw new RuntimeException(msg);
            }
            return this;
        }

        public Builder withResourcePath(final String resourcePath) {
            this.resourcePath = Paths.get(resourcePath);
            if (Files.notExists(this.resourcePath)) {
                final String msg = String.format("ResourcePath=%s does not exist", this.resourcePath);
                logger.error(msg);
                throw new RuntimeException(msg);
            }
            return this;
        }

        public BayLeafStaticContentServer build() {
            final Server server = new Server();

            HttpConfiguration httpConfig = null;
            if (this.port != -1) {
                 httpConfig = new HttpConfiguration();
                if (this.securePort != -1) {
                    httpConfig.setSecureScheme("https");
                    httpConfig.setSecurePort(this.securePort);
                }
                httpConfig.setOutputBufferSize(32768);

                final ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
                httpConnector.setPort(this.port);
                httpConnector.setName("HTTP");

                server.addConnector(httpConnector);
            }

            if (this.securePort != -1) {
                Objects.requireNonNull(this.keystore, () -> "Keystore is required when configuring SSL HTTP connector");

                final SslContextFactory sslContextFactory = new SslContextFactory.Server();
                sslContextFactory.setKeyStorePath(this.keystore.toString());
                sslContextFactory.setKeyStorePassword(this.keystorePass);
                sslContextFactory.setKeyManagerPassword(this.keystorePass);

                final HttpConfiguration httpsConfig = Objects.nonNull(httpConfig) ? new HttpConfiguration(httpConfig) : new HttpConfiguration();
                final SecureRequestCustomizer src = new SecureRequestCustomizer();
                src.setStsMaxAge(2000);
                src.setStsIncludeSubDomains(true);
                httpsConfig.addCustomizer(src);

                final ServerConnector httpsConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfig));
                httpsConnector.setPort(this.securePort);
                httpsConnector.setIdleTimeout(500000);

                server.addConnector(httpsConnector);
            }

            final ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setBaseResource(new PathResource(this.resourcePath.toFile()));

            final HandlerCollection handlerCollection = new HandlerCollection();
            final ContextHandler contextHandler = new ContextHandler("/" + this.context);
            contextHandler.setHandler(resourceHandler);

            handlerCollection.addHandler(contextHandler);

            server.setHandler(handlerCollection);

            return new BayLeafStaticContentServer(server);
        }
    }
}
