package io.videointel.user.application;

import io.videointel.shared.ownership.Owner;

public interface OwnerLookupPort {
    boolean exists(Owner owner);
}
