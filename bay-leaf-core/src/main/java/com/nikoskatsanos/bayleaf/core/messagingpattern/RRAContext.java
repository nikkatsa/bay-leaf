package com.nikoskatsanos.bayleaf.core.messagingpattern;

import com.nikoskatsanos.bayleaf.core.Session;
import java.util.function.Consumer;

public interface RRAContext<REQUEST, RESPONSE> {

    Session session();

    void onRequest(final Consumer<Request<REQUEST>> requestConsumer);

    void onAck(final Consumer<Void> ackConsumer);

    void onAckTimeout(final Consumer<Void> ackTimeoutConsumer);

    void response(final Response<REQUEST, RESPONSE> response);

    void error(final int errorCode, final String errorMsg, final Response<REQUEST, RESPONSE> errorResponse);
}
