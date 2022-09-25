package com.seregy77.ocr.domain.api.vision;

import java.util.List;

public record VisionWord(VisionBoundingPoly boundingBox, List<VisionSymbol> symbols) {
}
