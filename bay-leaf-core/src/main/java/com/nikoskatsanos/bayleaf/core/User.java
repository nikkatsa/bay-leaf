package com.nikoskatsanos.bayleaf.core;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Principal {

    private String username;
    private String userId;

    private final Set<String> roles = new HashSet<>();

    @Override
    public String getName() {
        return this.username;
    }

    public final boolean hasRole(final String role) {
        return this.roles.contains(role);
    }

    public final Set<String> getRoles() {
        return Collections.unmodifiableSet(this.roles);
    }

    public final void addRoles(final Collection<String> roles) {
        this.roles.addAll(roles);
    }
}
