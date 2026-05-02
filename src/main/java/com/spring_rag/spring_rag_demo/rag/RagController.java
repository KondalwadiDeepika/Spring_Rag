package com.spring_rag.spring_rag_demo.rag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public RagResponse generate(@RequestBody MessageRequest request) {
        return ragService.retrieveAndGenerate(request.message());
    }

    public static record MessageRequest(String message) {}

    public static record RagResponse(
        String answer,
        java.util.List<String> sources,
        String confidence,
        double confidence_score,
        long response_time_ms
    ) {}
}