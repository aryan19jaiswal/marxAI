package com.marxAI.model.chunking;

import java.util.List;

/**
 * Plain text extracted from an uploaded file by {@code TikaDocumentParser}, split per source page.
 * Formats without a native page concept (txt, md) come back as a single-element {@code pages} list.
 *
 * @param fullText all pages joined together, for callers that don't care about page boundaries
 * @param pages page-ordered text, one entry per page (1-indexed page number = list index + 1)
 */
public record ParsedDocument(String fullText, List<String> pages) {}
