package com.workmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Workmate — multi-tenant agentic AI platform.
 *
 * <p>Employees ask in natural language; the backend searches internal documents (RAG)
 * and internal databases (Text-to-SQL) and answers via SSE streaming.
 */
@EnableKafka
@SpringBootApplication
public class WorkmateApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkmateApplication.class, args);
    }
}
