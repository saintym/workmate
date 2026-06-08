package com.workmate.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Request body for the POST /api/v1/chat/messages endpoint.
 *
 * @param conversationId optional; when null a new conversation is created
 * @param content        the user message text; must not be blank
 */
public record SendMessageRequest(UUID conversationId, @NotBlank String content) {}
