package com.nikoskatsanos.bayleaf.domain.message;

public enum MessageType {
    // Session level
    SESSION_INITIALIZING,
    SESSION_INITIALIZED,
    AUTH,
    SERVICE_CREATE,
    HEARTBEAT,

    // Application level
    INITIAL_DATA,
    DATA,
    DATA_ACK,
    DATA_CLOSE,
    ERROR;
}
