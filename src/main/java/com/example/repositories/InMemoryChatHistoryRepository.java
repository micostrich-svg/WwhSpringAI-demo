package com.example.repositories;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Repository;

/**
 * 进程内的 {@link ChatHistoryRepository} 实现：重启即清空。
 * 需要持久化时换掉这个实现即可（接口不用动）。
 */
@Repository
public class InMemoryChatHistoryRepository implements ChatHistoryRepository {

    private final Map<String, List<String>> chatHistory = new ConcurrentHashMap<>();

    @Override
    public void save(String type, String chatId) {
        List<String> chatIds = this.chatHistory.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>());
        if (!chatIds.contains(chatId)) {
            chatIds.add(chatId);
        }
    }

    @Override
    public List<String> getChatIds(String type) {
        List<String> chatIds = this.chatHistory.get(type);
        return chatIds != null ? List.copyOf(chatIds) : List.of();
    }

}
