package com.seregy77.ocr.mapper;

import com.seregy77.ocr.domain.api.vision.*;
import com.seregy77.ocr.domain.document.BoundingBox;
import com.seregy77.ocr.domain.document.Page;
import com.seregy77.ocr.domain.document.Position;
import com.seregy77.ocr.domain.document.Word;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ApiResponseMapper {
    public List<Page> mapToPages(VisionAnnotateResponse annotateResponse) {
        List<Page> result = new ArrayList<>();

        List<VisionPage> pages = annotateResponse.fullTextAnnotation().pages();
        for (int i = 0; i < pages.size(); i++) {
            VisionPage visionPage = pages.get(i);
            result.add(mapToPage(i, visionPage));
        }

        return result;
    }

    private Page mapToPage(int pageIndex, VisionPage page) {
        List<Word> words = page.blocks().stream()
                .map(VisionBlock::paragraphs)
                .flatMap(Collection::stream)
                .map(VisionParagraph::words)
                .flatMap(Collection::stream)
                .map(this::mapToDocumentSymbols)
                .toList();

        return new Page(pageIndex, words);
    }

    private Word mapToDocumentSymbols(VisionWord word) {
        List<VisionVertex> vertices = word.boundingBox().normalizedVertices();
        BoundingBox boundingBox = new BoundingBox(mapToPosition(vertices.get(0)), mapToPosition(vertices.get(1)),
                mapToPosition(vertices.get(2)), mapToPosition(vertices.get(3)));

        String wordText = word.symbols().stream()
                .map(VisionSymbol::text)
                .collect(Collectors.joining());

        return new Word(boundingBox, wordText);
    }

    private Position mapToPosition(VisionVertex vertex) {
        return new Position(vertex.x(), vertex.y());
    }
}
