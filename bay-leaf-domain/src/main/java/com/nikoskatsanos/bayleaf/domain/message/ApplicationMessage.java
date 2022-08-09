package com.nikoskatsanos.bayleaf.domain.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMessage implements Message{

    private String correlationId;
    private MessageType messageType;

    private String serviceName;
    private String route;
    private MessagingPattern messagingPattern;

    private byte[] data;
}
