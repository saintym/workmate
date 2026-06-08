package com.workmate.domain.knowledge;

/**
 * Lifecycle state of a {@link Document} through the indexing pipeline.
 *
 * <ul>
 *   <li>{@link #UPLOADED} — file stored in S3, not yet enqueued for indexing</li>
 *   <li>{@link #INDEXING} — chunking and embedding in progress</li>
 *   <li>{@link #INDEXED} — all chunks embedded and stored; document is searchable</li>
 *   <li>{@link #FAILED} — indexing pipeline encountered an unrecoverable error</li>
 * </ul>
 */
public enum DocumentStatus {
    UPLOADED,
    INDEXING,
    INDEXED,
    FAILED
}
