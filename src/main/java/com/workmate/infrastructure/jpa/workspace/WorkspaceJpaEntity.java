package com.workmate.infrastructure.jpa.workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    /** WorkspaceSettings map serialised as a JSON string. */
    @Column(name = "settings", columnDefinition = "text")
    private String settings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
