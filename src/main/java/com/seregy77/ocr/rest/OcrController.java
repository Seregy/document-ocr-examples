package com.seregy77.ocr.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seregy77.ocr.domain.api.vision.VisionAnnotateResponse;
import com.seregy77.ocr.domain.api.vision.VisionResponse;
import com.seregy77.ocr.domain.document.Page;
import com.seregy77.ocr.mapper.ApiResponseMapper;
import com.seregy77.ocr.service.DocumentTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
public class OcrController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrController.class);

    private final ApiResponseMapper apiResponseMapper;
    private final DocumentTransformer documentTransformer;
    private final ObjectMapper objectMapper;

    public OcrController(ApiResponseMapper apiResponseMapper, DocumentTransformer documentTransformer,
                         ObjectMapper objectMapper) {
        this.apiResponseMapper = apiResponseMapper;
        this.documentTransformer = documentTransformer;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/document", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> transformToSearchableDocument(@RequestParam(value = "pdf", required = false) MultipartFile pdfDocumentFile,
                                                                  @RequestParam(value = "ocr-json", required = false) MultipartFile ocrResponseJsonFile) {
        Optional<VisionResponse> visionResponse = extractVisionResponse(ocrResponseJsonFile);
        if (visionResponse.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Page> pages = apiResponseMapper.mapToPages(extractAnnotateResponse(visionResponse.get()));
        Optional<Resource> searchableDocument = documentTransformer.getSearchableDocument(pdfDocumentFile.getResource(), pages);

        return searchableDocument.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    private Optional<VisionResponse> extractVisionResponse(MultipartFile ocrResponseJsonFile) {
        try (var inputStream = ocrResponseJsonFile.getInputStream()) {
            return Optional.of(objectMapper.readValue(inputStream, VisionResponse.class));
        } catch (IOException e) {
            LOGGER.warn("Error on extracting the vision response from the JSON", e);
        }

        return Optional.empty();
    }

    private VisionAnnotateResponse extractAnnotateResponse(VisionResponse visionResponse) {
        return visionResponse.responses().get(0);
    }
}
