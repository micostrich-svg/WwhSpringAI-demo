package com.example.entity.vo;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

/**
 * Transport representation of a single chat message.
 */
public class MessageVO {

    private final String role;

    private final String content;

    public MessageVO(Message message) {
        this.role = toRole(message.getMessageType());
        this.content = message.getText();
    }

    public String getRole() {
        return this.role;
    }

    public String getContent() {
        return this.content;
    }

    private static String toRole(MessageType type) {
        return switch (type) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }

}
