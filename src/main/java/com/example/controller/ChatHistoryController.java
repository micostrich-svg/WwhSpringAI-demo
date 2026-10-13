package com.example.controller;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.entity.vo.MessageVO;
import com.example.repositories.ChatHistoryRepository;

@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {

    private final ChatHistoryRepository chatHistoryRepository;

    private final ChatMemory chatMemory;

    public ChatHistoryController(ChatHistoryRepository chatHistoryRepository, ChatMemory chatMemory) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.chatMemory = chatMemory;
    }

    /**
     * Lists the conversation ids recorded for one conversation type, for example
     * {@code /ai/history/chat}.
     */
    @GetMapping("/{type}")
    public List<String> getChatIds(@PathVariable String type) {
        return this.chatHistoryRepository.getChatIds(type);
    }

    /**
     * Returns the remembered messages of one conversation, oldest first. The conversation
     * type is part of the URL only; the memory itself is keyed by the conversation id.
     * <p>
     * {@link ChatMemory#get(String)} never returns {@code null}: an unknown conversation
     * yields an empty list.
     */
    @GetMapping("/{type}/{chatId}")
    public List<MessageVO> getChatMessages(@PathVariable String chatId) {
        List<Message> messages = this.chatMemory.get(chatId);
        return messages.stream().map(MessageVO::new).toList();
    }

}
