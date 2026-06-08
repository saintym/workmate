package com.workmate.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA / Spring Data configuration.
 *
 * <p>Narrows both the entity scan and the repository scan to the
 * {@code com.workmate.infrastructure.jpa} package so that only infrastructure
 * persistence classes are picked up. {@link org.springframework.boot.autoconfigure.SpringBootApplication}
 * on {@code WorkmateApplication} already provides broad component scanning; this bean
 * restricts JPA-specific scanning without interfering with it.
 *
 * <p>{@link EnableJpaAuditing} activates {@code @CreatedDate} / {@code @LastModifiedDate}
 * support for JPA entity auditing.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.workmate.infrastructure.jpa")
@EntityScan(basePackages = "com.workmate.infrastructure.jpa")
@EnableJpaAuditing
public class JpaConfig {
}
