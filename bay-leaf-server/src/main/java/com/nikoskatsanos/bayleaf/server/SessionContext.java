package com.nikoskatsanos.bayleaf.server;

import com.nikoskatsanos.bayleaf.domain.message.Heartbeat;
import com.nikoskatsanos.bayleaf.domain.Session;
import java.util.function.Consumer;

public interface SessionContext {

    Session getSession();

    void heartbeat(final Heartbeat heartbeat);

    void onHeartbeat(final String serviceName, final Consumer<Heartbeat> heartbeatConsumer);

    void closeSession(final byte closeCode);
}
