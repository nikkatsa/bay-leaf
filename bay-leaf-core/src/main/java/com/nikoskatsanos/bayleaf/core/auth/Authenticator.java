package com.nikoskatsanos.bayleaf.core.auth;

import com.nikoskatsanos.bayleaf.core.User;
import com.nikoskatsanos.bayleaf.core.auth.Authenticator.Token;

public interface Authenticator<T extends Token> {

    interface Token {
    }

    User authenticate(final T token);
}
