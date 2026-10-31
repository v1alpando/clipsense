package io.videointel.analysis.domain;

public record TranscriptSegment(double start, double end, String text) {

    public TranscriptSegment {
        if (text == null) {
            text = "";
        }
    }
}
