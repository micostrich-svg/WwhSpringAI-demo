package com.example.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.repositories.ChatHistoryRepository;

import reactor.core.publisher.Flux;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/ai")
public class ChatController {

    /**
     * 会话类型：会话 id 记录在这个分类下，用于 /ai/history/{type} 列表。
     */
    static final String CHAT_TYPE = "chat";

    private final ChatClient chatClient;

    private final ChatHistoryRepository chatHistoryRepository;

    public ChatController(ChatClient chatClient, ChatHistoryRepository chatHistoryRepository) {
        this.chatClient = chatClient;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    /**
     * 流式返回回答，每块一行 {@code data:}。
     * <p>
     * GET 与 POST 都走这里，参数从 query string 或表单体（含 multipart/form-data）读取，
     * 所以前端不管用 query、FormData 还是 urlencoded 提交都能直接用。
     * <p>
     * {@code chatId} 相同的请求共享同一段会话记忆，它通过
     * {@link ChatMemory#CONVERSATION_ID} 传给记忆顾问——这个参数不能少，缺了就相当于没有记忆。
     */
    @RequestMapping(
            value = "/chat",
            method = { RequestMethod.GET, RequestMethod.POST },
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> chat(
            @RequestParam(defaultValue = "讲个笑话") String prompt,
            @RequestParam String chatId
    ) {
        if (!StringUtils.hasText(prompt) || !StringUtils.hasText(chatId)) {
            throw new ResponseStatusException(BAD_REQUEST, "prompt 和 chatId 不能为空");
        }
        String conversationId = chatId.trim();
        this.chatHistoryRepository.save(CHAT_TYPE, conversationId);
        return this.chatClient.prompt(prompt)
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

}
