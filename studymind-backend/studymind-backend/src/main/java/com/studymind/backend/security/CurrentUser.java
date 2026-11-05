package com.studymind.backend.security;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    public UUID id() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
