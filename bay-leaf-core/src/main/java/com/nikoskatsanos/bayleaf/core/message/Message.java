package com.nikoskatsanos.bayleaf.core.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.EXISTING_PROPERTY, property = "messageType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SessionMessage.class, names = "SESSION_INITIALIZING"),
    @JsonSubTypes.Type(value = SessionMessage.class, names = "SESSION_INITIALIZED"),
    @JsonSubTypes.Type(value = SessionMessage.class, names = "AUTH"),
    @JsonSubTypes.Type(value = SessionMessage.class, names = "SERVICE_CREATE"),
    @JsonSubTypes.Type(value = SessionMessage.class, names = "HEARTBEAT"),
    @JsonSubTypes.Type(value = ApplicationMessage.class, names = {"INITIAL_DATA", "DATA", "DATA_ACK", "DATA_CLOSE"})}
)
public interface Message {

    String getCorrelationId();

    MessageType getMessageType();

    byte[] getData();
}
