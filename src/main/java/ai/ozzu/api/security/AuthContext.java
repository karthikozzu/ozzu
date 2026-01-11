package ai.ozzu.api.security;

import ai.ozzu.api.exceptions.MissingFieldException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthContext {

    public UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new MissingFieldException("Missing auth context");
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof String s) || s.isBlank()) {
            throw new MissingFieldException("Invalid auth principal");
        }

        try {
            return UUID.fromString(s.trim());
        } catch (IllegalArgumentException e) {
            throw new MissingFieldException("Invalid userId in auth token");
        }
    }
}