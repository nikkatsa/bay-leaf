package com.nikoskatsanos.bayleaf.client.auth;

import com.nikoskatsanos.bayleaf.core.auth.Authenticator.Token;
import com.nikoskatsanos.bayleaf.core.auth.Tokens;
import lombok.RequiredArgsConstructor;

/**
 * A provider/factory of the client's {@link com.nikoskatsanos.bayleaf.core.auth.Authenticator.Token} that will be used during the BayLeaf session initialization phase
 */
public interface ClientAuthTokenProvider {

    Token authToken();

    @RequiredArgsConstructor
    class UsernamePasswordClientAuthTokenProvider implements ClientAuthTokenProvider {

        private final String username;
        private final String password;

        @Override
        public Token authToken() {
            return new Tokens.UsernamePassword(username, password);
        }
    }
}
