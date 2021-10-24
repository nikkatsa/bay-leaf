package com.nikoskatsanos.bayleaf.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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
