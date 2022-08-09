package com.nikoskatsanos.bayleaf.core.auth;

import com.nikoskatsanos.bayleaf.domain.User;

public class NullAuthorizer implements Authorizer{

    @Override
    public String[] authorize(User user) {
        return new String[0];
    }
}
