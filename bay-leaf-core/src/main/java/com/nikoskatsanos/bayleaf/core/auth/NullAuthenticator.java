package com.nikoskatsanos.bayleaf.core.auth;

import com.nikoskatsanos.bayleaf.core.User;
import com.nikoskatsanos.bayleaf.core.auth.Tokens.UsernamePassword;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullAuthenticator implements Authenticator<UsernamePassword> {

    @Override
    public User authenticate(final UsernamePassword token) {
        Objects.requireNonNull(token.getUsername(), "Username must not be null");
        Objects.requireNonNull(token.getPassword(), "Password must not be null");
        logger.info("Authenticating Token={}", token);
        return new User(token.getUsername(), token.getPassword());
    }
}
