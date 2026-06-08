package com.workmate.domain.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ChunkingService}.
 */
class ChunkingServiceTest {

    private final ChunkingService service = new ChunkingService();

    // -----------------------------------------------------------------------
    // blank / empty input
    // -----------------------------------------------------------------------

    @Test
    void blank_text_returns_empty_list() {
        assertThat(service.chunk("   ", ChunkingStrategy.defaultStrategy())).isEmpty();
    }

    @Test
    void null_text_returns_empty_list() {
        assertThat(service.chunk(null, ChunkingStrategy.defaultStrategy())).isEmpty();
    }

    @Test
    void empty_string_returns_empty_list() {
        assertThat(service.chunk("", ChunkingStrategy.defaultStrategy())).isEmpty();
    }

    // -----------------------------------------------------------------------
    // short text (shorter than chunkSize) → single chunk
    // -----------------------------------------------------------------------

    @Test
    void short_text_returns_single_chunk() {
        String text = "Hello world";
        ChunkingStrategy strategy = new ChunkingStrategy(500, 50);
        List<String> chunks = service.chunk(text, strategy);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("Hello world");
    }

    @Test
    void text_exactly_chunkSize_returns_single_chunk() {
        // 10-char text, chunkSize=10 → one chunk
        String text = "0123456789";
        ChunkingStrategy strategy = new ChunkingStrategy(10, 2);
        List<String> chunks = service.chunk(text, strategy);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text);
    }

    // -----------------------------------------------------------------------
    // multiple chunks with correct overlap
    // -----------------------------------------------------------------------

    @Test
    void long_text_produces_multiple_chunks_with_correct_count() {
        // text length=30, chunkSize=10, overlap=2 → step=8
        // windows: [0,10), [8,18), [16,26), [24,30)  → 4 chunks
        String text = "abcdefghijklmnopqrstuvwxyz1234";  // 30 chars
        ChunkingStrategy strategy = new ChunkingStrategy(10, 2);
        List<String> chunks = service.chunk(text, strategy);
        assertThat(chunks).hasSize(4);
    }

    @Test
    void consecutive_chunks_share_overlap_chars() {
        // chunkSize=10, overlap=3 → step=7
        // chunk[0] = text[0..9], chunk[1] = text[7..16]
        // trailing 3 chars of chunk[0] == leading 3 chars of chunk[1]
        String text = "abcdefghijklmnopqrstuvwxyz";  // 26 chars
        ChunkingStrategy strategy = new ChunkingStrategy(10, 3);
        List<String> chunks = service.chunk(text, strategy);

        assertThat(chunks.size()).isGreaterThan(1);
        for (int i = 0; i < chunks.size() - 1; i++) {
            String current = chunks.get(i);
            String next = chunks.get(i + 1);
            // trailing `overlap` chars of current must equal leading `overlap` chars of next
            // (only valid when both chunks are full-width)
            if (current.length() == 10 && next.length() >= 3) {
                String trailingCurrent = current.substring(current.length() - 3);
                String leadingNext = next.substring(0, 3);
                assertThat(leadingNext)
                        .as("chunk[%d] trailing 3 chars should equal chunk[%d] leading 3 chars", i, i + 1)
                        .isEqualTo(trailingCurrent);
            }
        }
    }

    @Test
    void first_and_last_chunk_cover_full_text() {
        String text = "abcdefghijklmnopqrstuvwxyz";  // 26 chars
        ChunkingStrategy strategy = new ChunkingStrategy(10, 3);
        List<String> chunks = service.chunk(text, strategy);

        // first chunk starts at beginning
        assertThat(chunks.get(0)).startsWith("abc");
        // last chunk ends at the end of the trimmed text
        assertThat(chunks.get(chunks.size() - 1)).endsWith("xyz");
    }

    // -----------------------------------------------------------------------
    // exact boundary: text length == chunkSize + 1
    // -----------------------------------------------------------------------

    @Test
    void text_one_char_longer_than_chunkSize_produces_two_chunks() {
        // text=11 chars, chunkSize=10, overlap=0 → step=10
        // chunk[0]=[0,10), chunk[1]=[10,11) → 2 chunks
        String text = "01234567890";  // 11 chars
        ChunkingStrategy strategy = new ChunkingStrategy(10, 0);
        List<String> chunks = service.chunk(text, strategy);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).isEqualTo("0123456789");
        assertThat(chunks.get(1)).isEqualTo("0");
    }

    // -----------------------------------------------------------------------
    // zero overlap (non-overlapping windows)
    // -----------------------------------------------------------------------

    @Test
    void zero_overlap_produces_non_overlapping_chunks() {
        String text = "abcdefghij";  // 10 chars
        ChunkingStrategy strategy = new ChunkingStrategy(5, 0);
        List<String> chunks = service.chunk(text, strategy);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).isEqualTo("abcde");
        assertThat(chunks.get(1)).isEqualTo("fghij");
    }

    // -----------------------------------------------------------------------
    // null strategy throws
    // -----------------------------------------------------------------------

    @Test
    void null_strategy_throws_domain_exception() {
        assertThatThrownBy(() -> service.chunk("some text", null))
                .isInstanceOf(com.workmate.domain.common.DomainException.class)
                .hasMessageContaining("ChunkingStrategy");
    }

    // -----------------------------------------------------------------------
    // whitespace trimming
    // -----------------------------------------------------------------------

    @Test
    void leading_trailing_whitespace_is_trimmed_before_chunking() {
        String text = "  hello  ";
        ChunkingStrategy strategy = new ChunkingStrategy(10, 0);
        List<String> chunks = service.chunk(text, strategy);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("hello");
    }
}
