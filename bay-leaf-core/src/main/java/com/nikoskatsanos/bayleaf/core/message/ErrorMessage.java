package com.nikoskatsanos.bayleaf.core.message;

import com.nikoskatsanos.bayleaf.core.messagingpattern.MessagingPattern;
import lombok.Data;

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
