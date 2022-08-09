package com.nikoskatsanos.bayleaf.server.messagingpattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * An annotation to be used for {@link com.nikoskatsanos.bayleaf.server.Connector} endpoints that are request-response-ack(client side ack)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RRA {

    /**
     * @return the name of the RRA endpoint
     */
    String name();

    /**
     * @return type of incoming request
     */
    Class<?> requestType();

    /**
     * @return type of outgoing response
     */
    Class<?> responseType();

    /**
     * @return the timeout to wait for receiving the ack from the client
     */
    int ackTimeout() default 10;

    /**
     * @return the timeout unit to wait for receiving the ack from the client
     */
    TimeUnit ackTimeoutUnit() default TimeUnit.SECONDS;
}
