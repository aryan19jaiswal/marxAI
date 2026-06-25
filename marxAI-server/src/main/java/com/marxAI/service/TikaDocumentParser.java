package com.marxAI.service;

import com.marxAI.exception.DocumentParsingException;
import com.marxAI.model.chunking.ParsedDocument;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

/**
 * Extracts plain text from an uploaded file (PDF, DOCX, TXT, MD, ...) using Tika's format
 * auto-detection. PDFs are split into one text block per page (via PDFBox directly, since Tika's
 * SAX output doesn't expose page boundaries as plain text); other formats come back as a single
 * page, since they have no reliable page concept once converted to text.
 */
@Service
public class TikaDocumentParser {

    private static final String PDF_MEDIA_TYPE = "application/pdf";

    private final Tika tika = new Tika();
    private final Parser parser = new AutoDetectParser();

    /**
     * @param content the file's raw bytes, fully consumed and buffered before parsing
     * @param filename original filename, used as a detection hint and in error messages
     * @return extracted text, paginated for PDFs
     * @throws DocumentParsingException if the content can't be read or Tika fails to parse it
     */
    public ParsedDocument parse(InputStream content, String filename) {
        byte[] bytes = readAllBytes(content, filename);
        String mediaType = detectMediaType(bytes, filename);
        List<String> pages =
                PDF_MEDIA_TYPE.equals(mediaType) ? extractPdfPages(bytes, filename) : List.of(extractPlainText(bytes, filename));
        return new ParsedDocument(String.join("\n\n", pages), pages);
    }

    private String detectMediaType(byte[] bytes, String filename) {
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            return tika.detect(stream, filename);
        } catch (IOException ex) {
            throw new DocumentParsingException(filename, ex);
        }
    }

    private List<String> extractPdfPages(byte[] bytes, String filename) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> pages = new ArrayList<>(document.getNumberOfPages());
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(document));
            }
            return pages;
        } catch (IOException ex) {
            throw new DocumentParsingException(filename, ex);
        }
    }

    private String extractPlainText(byte[] bytes, String filename) {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            parser.parse(stream, handler, metadata, new ParseContext());
            return handler.toString();
        } catch (IOException | SAXException | TikaException ex) {
            throw new DocumentParsingException(filename, ex);
        }
    }

    private byte[] readAllBytes(InputStream content, String filename) {
        try {
            return content.readAllBytes();
        } catch (IOException ex) {
            throw new DocumentParsingException(filename, ex);
        }
    }
}
