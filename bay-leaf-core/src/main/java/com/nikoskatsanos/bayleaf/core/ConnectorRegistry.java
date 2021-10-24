package com.nikoskatsanos.bayleaf.core;

import java.util.HashMap;
import java.util.Map;

public class ConnectorRegistry {

    private final Map<String, Connector> connectors = new HashMap<>();

    public void registerConnector(final Connector connector) {
        this.connectors.put(connector.getName(), connector);
        connector.start();
    }

    public boolean hasConnector(final String serviceName) {
        return this.connectors.containsKey(serviceName);
    }

    public Connector getConnector(final String serviceName) {
        return this.connectors.get(serviceName);
    }
}
