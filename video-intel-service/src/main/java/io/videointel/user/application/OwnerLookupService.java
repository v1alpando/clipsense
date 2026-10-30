package io.videointel.user.application;

import io.videointel.shared.ownership.Owner;
import io.videointel.user.domain.AnonymousSessionRepositoryPort;
import io.videointel.user.domain.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class OwnerLookupService implements OwnerLookupPort{
    private final AnonymousSessionRepositoryPort sessionRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    public boolean exists(Owner owner) {
        return switch (owner.type()) {
            case USER -> userRepositoryPort.existsById(owner.id());
            case ANONYMOUS_SESSION -> sessionRepositoryPort.existsById(owner.id());
        };
    }
}
