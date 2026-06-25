package com.marxAI.model.chunking;

/**
 * A token-bounded slice of a {@link ParsedDocument}'s text produced by {@code ChunkingService},
 * ready to be embedded and stored (as a {@code Chunk} entity + Qdrant vector) in the next pipeline
 * stage.
 *
 * @param chunkIndex position of this chunk within the whole document, in reading order
 * @param pageNumber 1-indexed source page this chunk's text came from
 * @param text the chunk's text content
 */
public record TextChunk(int chunkIndex, int pageNumber, String text) {}
