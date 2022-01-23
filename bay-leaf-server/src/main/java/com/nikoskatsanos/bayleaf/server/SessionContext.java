package com.nikoskatsanos.bayleaf.server;

import com.nikoskatsanos.bayleaf.core.Heartbeat;
import com.nikoskatsanos.bayleaf.core.Session;
import java.util.function.Consumer;

public interface SessionContext {

    Session getSession();

    void heartbeat(final Heartbeat heartbeat);

    void onHeartbeat(final String serviceName, final Consumer<Heartbeat> heartbeatConsumer);

    void closeSession(final byte closeCode);
}
