package com.nikoskatsanos.bayleaf.core.messagingpattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Private stream messaging pattern endpoint
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PS {

    /**
     * @return the name of the private stream endpoint
     */
    String name();
}
