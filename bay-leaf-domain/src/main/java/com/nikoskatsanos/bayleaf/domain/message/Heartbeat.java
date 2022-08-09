package com.nikoskatsanos.bayleaf.domain.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * The payload of a {@link SessionMessage} for {@link MessageType#HEARTBEAT}. Containing the service's name, the heartbeat's ID and the server's timestamp
 * <p>
 *     The heartbeat's {@code id} is important as it must be re-transmitted back from the client side, in order to ensure heartbeats are not lost,
 *     and also client is not falling back.
 * </p>
 */
@Data
public class Heartbeat {

    private final String serviceName;
    private final int id;
    private final long timestamp;

    @JsonCreator(mode = Mode.PROPERTIES)
    public Heartbeat(@JsonProperty("serviceName") String serviceName, @JsonProperty("id") int id, @JsonProperty("timestamp") long timestamp) {
        this.serviceName = serviceName;
        this.id = id;
        this.timestamp = timestamp;
    }
}
