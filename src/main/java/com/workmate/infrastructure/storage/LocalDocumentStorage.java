package com.workmate.infrastructure.storage;

import com.workmate.application.port.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Local-filesystem implementation of {@link DocumentStorage}.
 *
 * <p>Stands in for S3 in the demo: documents are written to and read from
 * {@code <java.io.tmpdir>/workmate-docs/<key>}. The key encodes workspace, document UUID,
 * and original file name so it is deterministically addressable.
 */
@Component
public class LocalDocumentStorage implements DocumentStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalDocumentStorage.class);

    private static final Path BASE_DIR =
            Path.of(System.getProperty("java.io.tmpdir"), "workmate-docs");

    /**
     * Writes {@code content} to a local file and returns the storage key.
     *
     * @param workspaceId the owning workspace
     * @param name        the original file name
     * @param contentType the MIME type of the file
     * @param content     the raw text content to persist
     * @return an opaque storage key (relative path under the base dir)
     */
    @Override
    public String store(UUID workspaceId, String name, String contentType, String content) {
        String key = "workspaces/" + workspaceId + "/documents/" + UUID.randomUUID() + "/" + name;
        Path target = BASE_DIR.resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
            log.info("Stored document locally: workspaceId={}, name={}, contentType={}, key={}",
                    workspaceId, name, contentType, key);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store document at key: " + key, e);
        }
        return key;
    }

    /**
     * Reads and returns the content previously written by {@link #store}.
     *
     * @param key the storage key returned by {@link #store}
     * @return the raw text content
     * @throws UncheckedIOException if the file cannot be read
     */
    @Override
    public String load(String key) {
        Path target = BASE_DIR.resolve(key);
        try {
            String content = Files.readString(target, StandardCharsets.UTF_8);
            log.debug("Loaded document from local storage: key={}, length={}", key, content.length());
            return content;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load document at key: " + key, e);
        }
    }
}
