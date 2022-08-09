package com.nikoskatsanos.bayleaf.domain.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Session level {@link Message}s are sent by the framework itself, in order to establish a session between client/server
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionMessage implements Message{

    private String correlationId;

    private MessageType messageType;

    private byte[] data;
}
