package com.seregy77.ocr.domain.document;

import java.util.List;

public record Page(int index, List<Word> words) {
}
