package com.nikoskatsanos.bayleaf.core.message;

import lombok.Data;

@Data
public class ServiceMessage {

    private String correlationId;
    private String serviceName;
}
