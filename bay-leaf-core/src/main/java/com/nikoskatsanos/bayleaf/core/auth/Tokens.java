package com.nikoskatsanos.bayleaf.core.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nikoskatsanos.bayleaf.core.auth.Authenticator.Token;
import lombok.Data;
import lombok.RequiredArgsConstructor;

public class Tokens {

    @Data
    public static class UsernamePassword implements Token {

        private final String username;
        private final String password;

        @JsonCreator(mode = Mode.PROPERTIES)
        public UsernamePassword(@JsonProperty("username") String username, @JsonProperty("password") String password) {
            this.username = username;
            this.password = password;
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class Jws implements Token {

        private final String jwsToken;
    }
}
