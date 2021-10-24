package com.nikoskatsanos.bayleaf.core.messagingpattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Request-Response-Ack (client side ack) messaging pattern endpoint
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RRA {

    /**
     * @return the name of the RRA endpoint
     */
    String name();

    /**
     * @return the timeout to wait for receiving the ack from the client
     */
    int ackTimeout() default 10;

    /**
     * @return the timeout unit to wait for receiving the ack from the client
     */
    TimeUnit ackTimeoutUnit() default TimeUnit.SECONDS;
}
