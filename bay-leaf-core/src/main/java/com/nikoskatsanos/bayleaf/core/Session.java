package com.nikoskatsanos.bayleaf.core;

import java.util.Objects;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Session {

    private final String sessionId;
    private User user;

    public boolean isAuthenticated() {
        return Objects.nonNull(this.user);
    }
}
