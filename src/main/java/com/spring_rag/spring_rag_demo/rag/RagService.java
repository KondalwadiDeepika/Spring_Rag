package com.sohamkamani.spring_rag_demo.rag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage; // Changed import
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/rag-prompt.st")
    private Resource ragPromptTemplate;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder
                .build();
        this.vectorStore = vectorStore;
    }

    public String retrieveAndGenerate(String message) {
        List<Document> similarDocuments = vectorStore
                .similaritySearch(SearchRequest
                        .builder()
                        .query(message)
                        .topK(4)
                        .build());
        System.out.println(">>> Similar documents: " + similarDocuments);
        String information = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(ragPromptTemplate);
        Prompt prompt = new Prompt(List.of(
                systemPromptTemplate.createMessage(
                        Map.of("information", information)),
                new UserMessage(message)));
        System.out.println(">>> Prompt: " + prompt.getContents());
        return chatClient.prompt(prompt).call().content(); // Changed ChatClient usage
    }
}
