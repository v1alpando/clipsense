package io.videointel.shared.security;

import io.videointel.shared.ownership.Owner;
import io.videointel.shared.ownership.OwnerType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class OwnerExtractor {

    public Optional<Owner> extract(UUID ownerId, OwnerType ownerType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID userId) {
            return Optional.of(Owner.of(OwnerType.USER, userId));
        }
        if (ownerId != null && ownerType != null) {
            return Optional.of(Owner.of(ownerType, ownerId));
        }
        return Optional.empty();
    }
}
