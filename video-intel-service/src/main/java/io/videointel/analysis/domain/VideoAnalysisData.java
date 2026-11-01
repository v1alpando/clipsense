package io.videointel.analysis.domain;

import java.util.List;

public record VideoAnalysisData(
        String summary,
        Hook hook,
        List<KeyMoment> keyMoments,
        List<String> takeaways
) {

    public VideoAnalysisData {
        keyMoments = keyMoments == null ? List.of() : List.copyOf(keyMoments);
        takeaways = takeaways == null ? List.of() : List.copyOf(takeaways);
    }
}
