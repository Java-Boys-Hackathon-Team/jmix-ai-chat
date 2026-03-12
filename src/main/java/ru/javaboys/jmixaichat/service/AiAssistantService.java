package ru.javaboys.jmixaichat.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
public class AiAssistantService {

    public Flux<String> chat(String userMessage) {
        String response = generateMockResponse(userMessage);
        String[] tokens = response.split("(?<=\\s)");

        return Flux.fromArray(tokens)
                .delayElements(Duration.ofMillis(50));
    }

    private String generateMockResponse(String userMessage) {
        return "Hello! I'm an AI assistant running on the **Jmix** platform. " +
                "This response is being streamed token by token, " +
                "creating a real-time typing effect similar to ChatGPT. " +
                "In a production application, this service would be connected " +
                "to an LLM provider like **Claude** from Anthropic. " +
                "You can ask me anything and I'll do my best to help!";
    }
}
