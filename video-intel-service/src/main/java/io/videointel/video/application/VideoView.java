package io.videointel.video.application;

import java.util.UUID;

public record VideoView(UUID id, boolean uploaded, String storageRef) {}
