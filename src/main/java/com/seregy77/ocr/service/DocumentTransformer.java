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
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Component
public class DocumentTransformer {

    private static final PDType1Font PDF_FONT = PDType1Font.HELVETICA;
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
        BigDecimal wordBoundingBoxWidth = word.boundingBox().getWidth(documentWidth);
        BigDecimal stringWidth = BigDecimal.valueOf(PDF_FONT.getStringWidth(word.text()));

        return wordBoundingBoxWidth.multiply(BigDecimal.valueOf(1000))
                .divide(stringWidth, 2, RoundingMode.DOWN)
                .floatValue();
    }

    private BigDecimal getScaledX(BigDecimal originalX, BigDecimal totalDocumentWidth) {
        return originalX.multiply(totalDocumentWidth);
    }

    private BigDecimal getScaledY(BigDecimal originalY, BigDecimal totalDocumentHeight) {
        return originalY.multiply(totalDocumentHeight);
    }
}
