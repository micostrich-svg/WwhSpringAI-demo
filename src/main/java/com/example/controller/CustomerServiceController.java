package com.example.controller;

import com.example.repositories.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class CustomerServiceController {

    private final ChatClient serviceChatClient;

    private final ChatHistoryRepository chatHistoryRepository;

    @RequestMapping(value = "/service", produces = "text/html;charset=utf-8")
    public Flux<String> service(String prompt, String chatId) {
        // 1.保存会话id
        chatHistoryRepository.save("service", chatId);
        String conversationId = chatId.trim();
        // 2.请求模型
        return serviceChatClient.prompt()
                .user(prompt)
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}