package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

/**
 * Test double for the remote model: records every prompt it receives and answers with a
 * canned assistant message, so no network access is required.
 */
class StubChatModel implements ChatModel {

    private final List<Prompt> prompts = new ArrayList<>();

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public ChatResponse call(Prompt prompt) {
        this.prompts.add(prompt);
        return new ChatResponse(List.of(new Generation(new AssistantMessage(answer()))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }

    List<Prompt> getPrompts() {
        return List.copyOf(this.prompts);
    }

    private String answer() {
        return "answer-" + this.counter.incrementAndGet();
    }

}
