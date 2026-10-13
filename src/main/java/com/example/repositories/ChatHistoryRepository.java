package com.example.repositories;

import java.util.List;

/**
 * Stores which conversation ids exist per conversation type (for example
 * {@code chat}), so the UI can list past conversations.
 */
public interface ChatHistoryRepository {

    void save(String type, String chatId);

    List<String> getChatIds(String type);

}
