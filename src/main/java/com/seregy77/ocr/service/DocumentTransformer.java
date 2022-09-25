package com.seregy77.ocr.service;

import com.seregy77.ocr.domain.document.Page;
import com.seregy77.ocr.domain.document.Word;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class DocumentTransformer {

    private static final PDType1Font PDF_FONT = PDType1Font.HELVETICA;
    private static final int INITIAL_FONT_SIZE = 12;
    private static final float FONT_SIZE_CHANGE_STEP = 0.25f;
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentTransformer.class);

    public Optional<Resource> getSearchableDocument(Resource originalDocument, List<Page> pagesWithText) {

        try (InputStream originalDocumentStream = originalDocument.getInputStream();
             PDDocument pdf = PDDocument.load(originalDocumentStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                addTextToDocumentPage(pdf, pagesWithText, i);
            }

            pdf.save(outputStream);

            return Optional.of(new ByteArrayResource(outputStream.toByteArray()));
        } catch (IOException e) {
            LOGGER.warn("Error on building a searchable document", e);
        }

        return Optional.empty();
    }

    private void addTextToDocumentPage(PDDocument pdfDocument, List<Page> pagesWithText, int pageIndex)
            throws IOException {
        PDPage originalPage = pdfDocument.getPage(pageIndex);
        Page pageWithText = pagesWithText.get(pageIndex);

        BigDecimal documentHeight = BigDecimal.valueOf(originalPage.getMediaBox().getHeight());
        BigDecimal documentWidth = BigDecimal.valueOf(originalPage.getMediaBox().getWidth());

        try (PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, originalPage,
                PDPageContentStream.AppendMode.PREPEND, false)) {
            contentStream.setRenderingMode(RenderingMode.NEITHER);

            for (var word : pageWithText.words()) {
                addWordToPage(contentStream, word, documentHeight, documentWidth);
            }
        }
    }

    private void addWordToPage(PDPageContentStream contentStream, Word wordToAdd, BigDecimal documentHeight,
                               BigDecimal documentWidth) throws IOException {
        contentStream.beginText();

        float fontSize = estimateFontSize(wordToAdd, documentWidth);
        contentStream.setFont(PDF_FONT, fontSize);

        BigDecimal scaledX = getScaledX(wordToAdd.boundingBox().bottomLeft().x(), documentWidth);
        BigDecimal scaledY = getScaledY(wordToAdd.boundingBox().bottomLeft().y(), documentHeight);

        contentStream.newLineAtOffset(scaledX.floatValue(), documentHeight.subtract(scaledY).floatValue());
        contentStream.showText(wordToAdd.text());

        contentStream.endText();
    }

    private float estimateFontSize(Word word, BigDecimal documentWidth) throws IOException {
        float wordBoundingBoxWidth = word.boundingBox().getWidth(documentWidth).floatValue();
        String text = word.text();

        float fontSize = INITIAL_FONT_SIZE;
        float textWidthWithCurrentSize = calculateTextWidth(text, fontSize);

        if (textWidthWithCurrentSize > wordBoundingBoxWidth) {
            while (textWidthWithCurrentSize > wordBoundingBoxWidth) {
                fontSize -= FONT_SIZE_CHANGE_STEP;
                textWidthWithCurrentSize = calculateTextWidth(text, fontSize);
            }
        } else if (textWidthWithCurrentSize < wordBoundingBoxWidth) {
            while (textWidthWithCurrentSize < wordBoundingBoxWidth) {
                fontSize += FONT_SIZE_CHANGE_STEP;
                textWidthWithCurrentSize = calculateTextWidth(text, fontSize);
            }
        }

        return fontSize;
    }

    private BigDecimal getScaledX(BigDecimal originalX, BigDecimal totalDocumentWidth) {
        return originalX.multiply(totalDocumentWidth);
    }

    private BigDecimal getScaledY(BigDecimal originalY, BigDecimal totalDocumentHeight) {
        return originalY.multiply(totalDocumentHeight);
    }

    private static float calculateTextWidth(String text, float fontSize) throws IOException {
        return PDF_FONT.getStringWidth(text) / 1000 * fontSize;
    }
}
