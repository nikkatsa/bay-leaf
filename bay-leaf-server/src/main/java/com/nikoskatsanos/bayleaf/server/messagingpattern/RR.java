package com.nikoskatsanos.bayleaf.server.messagingpattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to be used for {@link com.nikoskatsanos.bayleaf.server.Connector} endpoints that are request-response
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RR {

    /**
     * @return the name of the RR endpoint
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
}
