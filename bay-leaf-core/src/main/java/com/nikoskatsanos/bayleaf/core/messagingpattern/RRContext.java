package com.nikoskatsanos.bayleaf.core.messagingpattern;

import com.nikoskatsanos.bayleaf.core.Session;
import java.util.function.Consumer;

public interface RRContext<REQUEST, RESPONSE> {

    Session session();

    void onRequest(final Consumer<Request<REQUEST>> requestConsumer);

    void response(final Response<REQUEST, RESPONSE> response);

    void error(final int errorCode, final String errorMsg, final Response<REQUEST, RESPONSE> errorResponse);
}
