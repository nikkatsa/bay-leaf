package com.nikoskatsanos.bayleaf.domain.message;

import lombok.Data;

/**
 * A special {@link ApplicationMessage} to denote an error, with an error code and a reason.
 */
@Data
public class ErrorMessage extends ApplicationMessage{

    private int errorCode;
    private String errorMsg;

    public ErrorMessage(int errorCode, String errorMsg, String correlationId, String serviceName, String route, MessagingPattern messagingPattern) {
        super(correlationId, MessageType.ERROR, serviceName, route, messagingPattern, null);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }
}
