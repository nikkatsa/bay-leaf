package com.nikoskatsanos.bayleaf.core.auth;

import com.nikoskatsanos.bayleaf.core.User;

public interface Authorizer {

    String[] authorize(final User user);
}
