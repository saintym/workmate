package com.workmate.domain.conversation;

import com.workmate.domain.common.Entity;

import java.time.Instant;
import java.util.Objects;

/**
 * A single turn within a {@link Conversation}.
 *
 * <p>Messages are ordered by their {@code sequence} number, which is assigned by the
 * owning {@link Conversation} at the time of creation.
 */
public class Message extends Entity<MessageId> {

    private final MessageRole role;
    private final MessageContent content;
    private final int sequence;
    private final Instant createdAt;

    private Message(MessageId id, MessageRole role, MessageContent content, int sequence, Instant createdAt) {
        super(id);
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.sequence = sequence;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Create a new {@code Message}, stamping a fresh {@link MessageId} and the current
     * instant as {@code createdAt}.
     *
     * @param role     the author role
     * @param content  the message body
     * @param sequence the zero-based position of this message in its conversation
     * @return a new {@code Message}
     */
    public static Message create(MessageRole role, MessageContent content, int sequence) {
        return new Message(MessageId.newId(), role, content, sequence, Instant.now());
    }

    /**
     * Reconstitute a {@code Message} from persistent state (no side-effects).
     *
     * @param id        the persisted identifier
     * @param role      the author role
     * @param content   the message body
     * @param sequence  the zero-based position of this message in its conversation
     * @param createdAt the original creation timestamp
     * @return the reconstituted {@code Message}
     */
    public static Message reconstitute(MessageId id, MessageRole role, MessageContent content,
                                       int sequence, Instant createdAt) {
        return new Message(id, role, content, sequence, createdAt);
    }

    /** @return the role of the author of this message */
    public MessageRole role() {
        return role;
    }

    /** @return the text body of this message */
    public MessageContent content() {
        return content;
    }

    /** @return the zero-based position of this message within its conversation */
    public int sequence() {
        return sequence;
    }

    /** @return when this message was created */
    public Instant createdAt() {
        return createdAt;
    }
}
