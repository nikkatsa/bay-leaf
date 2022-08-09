package com.nikoskatsanos.bayleaf.domain;

import java.util.Objects;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * A bay-leaf session, having a session identifier and the user details associated with the session
 */
@Data
@RequiredArgsConstructor
public class Session {

    private final String sessionId;
    private User user;

    public boolean isAuthenticated() {
        return Objects.nonNull(this.user);
    }
}
