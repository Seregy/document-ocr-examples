package com.seregy77.ocr.domain.api.vision;

import java.util.List;

public record VisionResponse(List<VisionAnnotateResponse> responses) {
}
