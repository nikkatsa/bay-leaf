package com.nikoskatsanos.bayleaf.domain.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

/**
 * A polymorphic message that is exchanged between client/server. Every bay-leaf message needs to implement this interface
 * <p>
 *     Messages are separated into {@link SessionMessage}, which define the negotiation between client/server for establishing a session,
 *     and {@link ApplicationMessage}, which carry out application specific information and are sent after a session has been established.
 * </p>
 */
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

    /**
     * @return a unique id that identifies the message sent between the client and the server
     */
    String getCorrelationId();

    /**
     * @return The message's type, mainly used as a helper/indicator for serializing/deserializing this polymorphic type
     */
    MessageType getMessageType();

    /**
     * @return the message's payload.
     */
    byte[] getData();
}
