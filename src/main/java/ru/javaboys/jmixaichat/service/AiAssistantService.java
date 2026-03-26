package ru.javaboys.jmixaichat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
public class AiAssistantService {

    private final ChatClient chatClient;

    public AiAssistantService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Flux<String> chat(String userMessage, UUID conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId.toString()))
                .stream()
                .content();
    }
}
