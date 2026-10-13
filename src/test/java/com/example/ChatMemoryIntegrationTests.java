package com.example;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.example.controller.ChatController;
import com.example.entity.vo.MessageVO;
import com.example.repositories.InMemoryChatHistoryRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the chat wiring end to end without calling a real model: a conversation id
 * must reach the memory advisor and the memory must be replayed on the next turn of the
 * same conversation only.
 */
@SpringBootTest
class ChatMemoryIntegrationTests {

    private static final String CHAT_ID = "conversation-1";

    @Autowired
    private ChatController chatController;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private InMemoryChatHistoryRepository chatHistoryRepository;

    @Autowired
    private StubChatModel model;

    @Test
    void memoryIsScopedToTheConversationIdAndReplayedOnTheNextTurn() {
        assertThat(this.chatController.chat("你好", CHAT_ID).collectList().block()).containsExactly("answer-1");
        assertThat(this.chatController.chat("再见", CHAT_ID).collectList().block()).containsExactly("answer-2");

        List<Prompt> prompts = this.model.getPrompts();
        assertThat(prompts).hasSize(2);
        // First turn: system prompt plus the new user message.
        assertThat(roles(prompts.get(0))).containsExactly(MessageType.SYSTEM, MessageType.USER);
        // Second turn: the remembered user/assistant pair is prepended.
        assertThat(roles(prompts.get(1))).containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT,
                MessageType.USER);
        assertThat(texts(prompts.get(1))).containsExactly("你是一个小助手", "你好", "answer-1", "再见");

        // The history endpoint reads the same memory through the same keys.
        List<MessageVO> remembered = messagesOf(CHAT_ID);
        assertThat(remembered).extracting(MessageVO::getRole).containsExactly("user", "assistant", "user", "assistant");
        assertThat(remembered).extracting(MessageVO::getContent).containsExactly("你好", "answer-1", "再见", "answer-2");

        assertThat(this.chatHistoryRepository.getChatIds("chat")).containsExactly(CHAT_ID);
        // A different conversation starts empty.
        assertThat(messagesOf("another-conversation")).isEmpty();
    }

    private List<MessageVO> messagesOf(String chatId) {
        return this.chatMemory.get(chatId).stream().map(MessageVO::new).toList();
    }

    private static List<MessageType> roles(Prompt prompt) {
        return prompt.getInstructions().stream().map(Message::getMessageType).toList();
    }

    private static List<String> texts(Prompt prompt) {
        return prompt.getInstructions().stream().map(Message::getText).toList();
    }

    @TestConfiguration
    static class StubModelConfiguration {

        /**
         * The single {@link ChatModel} of this context: it replaces the
         * auto-configured OpenAI model, so no request ever leaves the JVM.
         */
        @Bean
        @Primary
        StubChatModel stubChatModel() {
            return new StubChatModel();
        }

    }

}
