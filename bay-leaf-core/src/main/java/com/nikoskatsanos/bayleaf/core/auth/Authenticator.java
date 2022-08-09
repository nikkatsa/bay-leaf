package com.nikoskatsanos.bayleaf.core.auth;

import com.nikoskatsanos.bayleaf.domain.User;
import com.nikoskatsanos.bayleaf.core.auth.Authenticator.Token;

/**
 * Authenticates a generic {@link Token} into a {@link User}
 * <p>
 * The token can be anything and is dependent on implementation:
 * <p>A username/password</p>
 * <p>A user certificate</p>
 * <p>A JWT (JSON Web Token)</p>
 * </p>
 *
 * @param <T> token type that the {@link Authenticator} operates upon
 */
public interface Authenticator<T extends Token> {

    interface Token {
    }

    /**
     * @param token to be used for authentication, based on the respective implementation
     * @return The authenticated {@link User}
     */
    User authenticate(final T token);
}
