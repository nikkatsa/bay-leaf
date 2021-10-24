package com.nikoskatsanos.bayleaf.core.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionMessage implements Message{

    private String correlationId;

    private MessageType messageType;

    private byte[] data;
}
