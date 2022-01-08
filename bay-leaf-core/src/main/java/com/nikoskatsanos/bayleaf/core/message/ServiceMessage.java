package com.nikoskatsanos.bayleaf.core.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMessage {

    private String correlationId;
    private String serviceName;
}
