package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marxAI.exception.DocumentParsingException;
import com.marxAI.model.chunking.ParsedDocument;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TikaDocumentParser}. PDFs are generated on the fly with PDFBox. */
class TikaDocumentParserTest {

    private final TikaDocumentParser parser = new TikaDocumentParser();

    @Test
    void parse_extractsPlainText_fromTxtFile() {
        InputStream content = stream("Binary search runs in O(log n) time.");

        ParsedDocument document = parser.parse(content, "notes.txt");

        assertThat(document.pages()).hasSize(1);
        assertThat(document.fullText()).contains("Binary search runs in O(log n) time.");
    }

    @Test
    void parse_extractsPlainText_fromMarkdownFile() {
        InputStream content = stream("# DSA Notes\n\nBinary trees are hierarchical structures.");

        ParsedDocument document = parser.parse(content, "notes.md");

        assertThat(document.pages()).hasSize(1);
        assertThat(document.fullText()).contains("DSA Notes").contains("Binary trees are hierarchical structures.");
    }

    @Test
    void parse_extractsSinglePage_fromSinglePagePdf() throws IOException {
        byte[] pdf = buildPdf("Only page content.");

        ParsedDocument document = parser.parse(new ByteArrayInputStream(pdf), "single.pdf");

        assertThat(document.pages()).hasSize(1);
        assertThat(document.pages().get(0)).contains("Only page content.");
    }

    @Test
    void parse_extractsOnePageOfTextPerPage_fromMultiPagePdf() throws IOException {
        byte[] pdf = buildPdf("First page content.", "Second page content.", "Third page content.");

        ParsedDocument document = parser.parse(new ByteArrayInputStream(pdf), "multi.pdf");

        assertThat(document.pages()).hasSize(3);
        assertThat(document.pages().get(0)).contains("First page content.");
        assertThat(document.pages().get(1)).contains("Second page content.");
        assertThat(document.pages().get(2)).contains("Third page content.");
        assertThat(document.pages().get(0)).doesNotContain("Second page content.");
        assertThat(document.fullText())
                .contains("First page content.")
                .contains("Second page content.")
                .contains("Third page content.");
    }

    @Test
    void parse_throwsDocumentParsingException_whenBytesAreCorrupt() {
        // Starts with the PDF magic header so Tika detects it as application/pdf, but the body
        // is garbage so PDFBox fails to parse a structure (no xref/trailer) out of it.
        InputStream corrupt =
                new ByteArrayInputStream("%PDF-1.4\nthis is not a valid pdf body".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parser.parse(corrupt, "broken.pdf"))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("broken.pdf");
    }

    private static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] buildPdf(String... pageTexts) throws IOException {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                pdf.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText(pageText);
                    stream.endText();
                }
            }
            pdf.save(out);
            return out.toByteArray();
        }
    }
}
