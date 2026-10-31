package io.videointel.video.application;

import io.videointel.shared.ownership.Owner;

import java.util.UUID;

public record VideoView(UUID id, boolean uploaded, String storageRef, Owner owner) {

    public VideoView(UUID id, boolean uploaded, String storageRef) {
        this(id, uploaded, storageRef, null);
    }
}
