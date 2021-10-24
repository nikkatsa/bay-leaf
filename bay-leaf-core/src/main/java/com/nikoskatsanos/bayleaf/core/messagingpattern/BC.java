package com.nikoskatsanos.bayleaf.core.messagingpattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Broadcast messaging pattern endpoint
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface BC {

    /**
     * @return the name of the BC endpoint
     */
    String name();
}
