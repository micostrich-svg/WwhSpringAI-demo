package com.example.config;

import com.example.constants.SystemConstants;
import com.example.tools.CourseTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.constants.SystemConstants.CUSTOMER_SERVICE_SYSTEM;

@Configuration
public class CommonConfiguration {

    /**
     * Number of messages kept per conversation; older messages are evicted.
     */
    private static final int MAX_MESSAGES = 20;

    /**
     * {@link MessageWindowChatMemory} replaces the removed {@code InMemoryChatMemory};
     * it keeps the last {@value #MAX_MESSAGES} messages of every conversation id.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(MAX_MESSAGES)
                .build();
    }

    /**
     * Built from the auto-configured {@link ChatClient.Builder} so that Spring AI's own
     * defaults (observations, tool calling, customizers) stay in place.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem("你是一个小助手")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean
    public ChatClient gameChatClient(OpenAiChatModel model, ChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                .defaultSystem(SystemConstants.GAME_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean
    public ChatClient serviceChatClient(
            OpenAiChatModel model,
            ChatMemory chatMemory,
            CourseTools courseTools) {
        return ChatClient.builder(model)
                .defaultSystem(CUSTOMER_SERVICE_SYSTEM)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(), // CHAT MEMORY
                        new SimpleLoggerAdvisor())
                .defaultTools(courseTools)
                .build();
    }

    /**
     * 内存向量库：进程重启即清空，够课程/开发用。
     * 生产要换成 PGVector / Milvus 之类的 starter。
     */
    @Bean
    public SimpleVectorStore vectorStore(OpenAiEmbeddingModel model) {
        return SimpleVectorStore.builder(model).build();
    }

    /**
     * RAG 问答用的 ChatClient：QuestionAnswerAdvisor 会在每次提问前先用
     * 用户问题去 {@link VectorStore} 里做相似度检索，把命中的文档片段拼进 prompt。
     * <p>
     * 2.0 起 {@code QuestionAnswerAdvisor} 只能通过 {@code builder} 创建，
     * 1.x 的 {@code new QuestionAnswerAdvisor(vectorStore, searchRequest)} 构造器已经没有了。
     */
    @Bean
    public ChatClient pdfChatClient(
            OpenAiChatModel model,
            ChatMemory chatMemory,
            VectorStore vectorStore) {
        return ChatClient.builder(model)
                .defaultSystem("请根据提供的上下文回答问题，不要自己猜测。")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(), // CHAT MEMORY
                        new SimpleLoggerAdvisor(),
                        QuestionAnswerAdvisor.builder(vectorStore) // 向量库
                                .searchRequest(SearchRequest.builder() // 向量检索的请求参数
                                        .similarityThreshold(0.5d) // 相似度阈值
                                        .topK(2) // 返回的文档片段数量
                                        .build())
                                .build())
                .build();
    }
}
