package com.workmate.infrastructure.storage;

import com.workmate.application.port.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LocalDocumentStorage implements DocumentStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalDocumentStorage.class);

    // TODO Phase 3: real S3 upload

    @Override
    public String store(UUID workspaceId, String name, String contentType) {
        String key = "workspaces/" + workspaceId + "/documents/" + UUID.randomUUID() + "/" + name;
        log.info("Storing document locally: workspaceId={}, name={}, contentType={}, key={}",
                workspaceId, name, contentType, key);
        return key;
    }
}
