package com.nikoskatsanos.bayleaf.domain.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A {@link ServiceMessage} is <b>not</b> a {@link SessionMessage} nor an {@link ApplicationMessage}. It is
 * a message represented by the {@link Message#getData()} on the {@link SessionMessage} and is received by a client for establishing a connection to a service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMessage {

    private String correlationId;
    private String serviceName;
}
