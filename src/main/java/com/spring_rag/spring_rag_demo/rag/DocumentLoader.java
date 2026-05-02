package com.spring_rag.spring_rag_demo.rag;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DocumentLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentLoader.class);

    private final VectorStore vectorStore;

    public DocumentLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        List<Document> existing = vectorStore.similaritySearch(
            SearchRequest.builder().query("StarlightDB").topK(1).build()
        );

        if (!existing.isEmpty()) {
            log.info("VectorStore already populated, skipping load.");
            return;
        }

        List<Document> documents = List.of(
            new Document("StarlightDB is a serverless graph database.", Map.of("source", "overview-doc")),
            new Document("It supports encryption using AES-256 at rest and TLS in transit.", Map.of("source", "security-doc")),
            new Document("It supports fast queries using indexing and parallel execution.", Map.of("source", "performance-doc")),
            new Document("It allows querying historical data using versioning features.", Map.of("source", "features-doc")),
            new Document("It provides role-based access control for security.", Map.of("source", "security-doc"))
        );

        vectorStore.add(documents);
        log.info("Documents loaded into VectorStore.");
    }
}