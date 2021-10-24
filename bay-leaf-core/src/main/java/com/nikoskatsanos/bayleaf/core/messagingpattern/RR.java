package com.nikoskatsanos.bayleaf.core.messagingpattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Request-Response messaging pattern endpoint
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RR{

    /**
     * @return the name of the RR endpoint
     */
    String name();
}
