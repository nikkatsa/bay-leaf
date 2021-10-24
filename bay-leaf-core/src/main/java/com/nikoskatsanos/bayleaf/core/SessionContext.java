package com.nikoskatsanos.bayleaf.core;

import java.util.function.Consumer;

public interface SessionContext {

    Session getSession();

    void heartbeat(final Heartbeat heartbeat);

    void onHeartbeat(final Consumer<Heartbeat> heartbeatConsumer);
}
