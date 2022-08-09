package com.nikoskatsanos.bayleaf.core.auth;

import com.nikoskatsanos.bayleaf.domain.User;

/**
 * Interface for authorization implementations
 */
public interface Authorizer {

    /**
     * @param user The {@link User} to authorize
     * @return an array with user permissions
     */
    String[] authorize(final User user);
}
