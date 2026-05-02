package com.spring_rag.spring_rag_demo.rag;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.spring_rag.spring_rag_demo.rag.RagController.RagResponse;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public RagResponse retrieveAndGenerate(String message) {

        // Step 0: Empty query
        if (message == null || message.trim().isEmpty()) {
            return new RagResponse(
                "Please provide a valid question.",
                List.of(), "low", 0.0, 0L
            );
        }

        long startTime = System.currentTimeMillis();
        log.info("Query: {}", message);

        // Step 1: Retrieve docs
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(message)
                .topK(3)
                .build()
        );

        log.info("Docs retrieved: {}", docs.size());
        docs.forEach(d -> log.info("DOC: {}", d.getText()));

        // Step 2: Fallback if no docs
        if (docs == null || docs.isEmpty()) {
            return new RagResponse(
                "I don't know based on the given data.",
                List.of(), "low", 0.0,
                System.currentTimeMillis() - startTime
            );
        }

        // Step 3: Build context
        String context = docs.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n"));

        log.info("===== CONTEXT =====");
        log.info(context);

        // Step 4: Sources
      // Use only the best matching document's source
// Pick most relevant source based on answer content
// Pick source from highest scoring non-overview doc
String bestSource = docs.stream()
    .map(d -> (String) d.getMetadata().get("source"))
    .filter(s -> s != null && !s.equals("overview-doc"))
    .findFirst()
    .orElse((String) docs.get(0).getMetadata().get("source"));

List<String> sources = bestSource != null ? List.of(bestSource) : List.of();
        // Step 5: Confidence
        double confidenceScore = docs.size() >= 2 ? 0.8 : 0.5;
        String confidence = confidenceScore >= 0.7 ? "high" :
                            confidenceScore >= 0.4 ? "medium" : "low";

        // Step 6: Strict prompt
        String prompt = "You are a strict RAG assistant.\n"
            + "Rules:\n"
            + "- Answer ONLY using the provided context below.\n"
            + "- Do NOT add any information not present in the context.\n"
            + "- Include ALL relevant details from the context.\n"
            + "- Answer in ONE concise sentence. Include only key facts.\n"
            + "- Do NOT start with Yes or No.\n"
            + "- Do NOT use phrases like 'this suggests' or 'this indicates'.\n"
            + "- If the answer is not clearly present in context, say exactly: "
            + "I don't know based on the given data.\n\n"
            + "Context:\n" + context + "\n\n"
            + "Question: " + message + "\n\n"
            + "Answer:";

        // Step 7: Call LLM
        String answer = chatClient.prompt(prompt).call().content();

        // Step 8: Output validation
        if (answer == null
                || answer.trim().isEmpty()
                || answer.trim().equals("0")
                || answer.trim().length() < 5) {
            log.warn("Invalid LLM output detected: {}", answer);
            return new RagResponse(
                "I don't know based on the given data.",
                List.of(), "low", 0.0,
                System.currentTimeMillis() - startTime
            );
        }

        long responseTime = System.currentTimeMillis() - startTime;

        log.info("ANSWER: {}", answer);
        log.info("SOURCES: {}", sources);
        log.info("CONFIDENCE: {} ({})", confidence, confidenceScore);
        log.info("RESPONSE TIME: {}ms", responseTime);

        // Step 9: Clear sources if LLM says it doesn't know
        if (answer.toLowerCase().contains("i don't know")) {
            return new RagResponse(
                "I don't know based on the given data.",
                List.of(), "low", 0.0,
                System.currentTimeMillis() - startTime
            );
        }

        return new RagResponse(answer, sources, confidence, confidenceScore, responseTime);
    }
}