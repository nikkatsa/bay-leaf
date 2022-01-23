package com.nikoskatsanos.bayleaf.server.messagingpattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to be used for {@link com.nikoskatsanos.bayleaf.core.Connector} methods that are shared stream endpoints
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SS {

    /**
     * @return endpoint's name
     */
    String name();

    /**
     * @return type of incoming subscription
     */
    Class<?> subscriptionType();

    /**
     * @return type of initial(snapshot) streamed data
     */
    Class<?> initialDataType();

    /**
     * @return type of streamed data
     */
    Class<?> dataType();
}
