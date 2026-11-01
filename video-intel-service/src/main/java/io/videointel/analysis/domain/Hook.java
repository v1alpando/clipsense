package io.videointel.analysis.domain;

public record Hook(
        int score,
        String type,
        String critique,
        String suggestion
) {
}
