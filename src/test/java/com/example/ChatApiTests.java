package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import com.example.entity.vo.MessageVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口契约测试：流式输出、参数校验、历史记录、会话之间的隔离。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ChatMemoryIntegrationTests.StubModelConfiguration.class)
class ChatApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatMemory chatMemory;

    @Test
    void getStreamsAnswer() throws Exception {
        assertStreamsAnswer(get("/ai/chat").param("prompt", "你好").param("chatId", "get-1"));
    }

    /** 前端用 FormData 提交，走的就是这条路径。 */
    @Test
    void multipartStreamsAnswer() throws Exception {
        assertStreamsAnswer(multipart("/ai/chat").param("prompt", "你好").param("chatId", "form-1"));
    }

    @Test
    void formUrlEncodedStreamsAnswer() throws Exception {
        assertStreamsAnswer(post("/ai/chat").contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("prompt", "你好")
            .param("chatId", "form-2"));
    }

    @Test
    void blankPromptOrMissingChatIdIsRejected() throws Exception {
        this.mockMvc.perform(get("/ai/chat").param("prompt", "  ").param("chatId", "bad-1"))
            .andExpect(status().isBadRequest());
        this.mockMvc.perform(get("/ai/chat").param("prompt", "你好"))
            .andExpect(status().isBadRequest());
        this.mockMvc.perform(multipart("/ai/chat").param("prompt", "你好"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void historyReturnsRememberedMessages() throws Exception {
        assertStreamsAnswer(get("/ai/chat").param("prompt", "你好").param("chatId", "web-3"));

        this.mockMvc.perform(get("/ai/history/chat"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        this.mockMvc.perform(get("/ai/history/chat/web-3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].role").value("user"))
            .andExpect(jsonPath("$[0].content").value("你好"))
            .andExpect(jsonPath("$[1].role").value("assistant"));

        this.mockMvc.perform(get("/ai/history/chat/unknown"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    /** 来回切换会话后，各自的记忆都还在，互不覆盖。 */
    @Test
    void eachConversationKeepsItsOwnMemory() throws Exception {
        chat("first question", "conversation-a");
        chat("other question", "conversation-b");
        chat("second question", "conversation-a");

        this.mockMvc.perform(get("/ai/history/chat/conversation-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].content").value("first question"))
            .andExpect(jsonPath("$[1].role").value("assistant"))
            .andExpect(jsonPath("$[2].content").value("second question"))
            .andExpect(jsonPath("$[3].role").value("assistant"))
            .andExpect(jsonPath("$[4]").doesNotExist());

        this.mockMvc.perform(get("/ai/history/chat/conversation-b"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].content").value("other question"))
            .andExpect(jsonPath("$[1].role").value("assistant"))
            .andExpect(jsonPath("$[2]").doesNotExist());
    }

    @Test
    void rejectedRequestDoesNotDisturbExistingMemory() throws Exception {
        chat("remember me", "keep-1");

        this.mockMvc.perform(get("/ai/chat").param("prompt", "  ").param("chatId", "keep-1"))
            .andExpect(status().isBadRequest());

        assertThat(texts("keep-1")).hasSize(2);
    }

    /**
     * 断言流式回答。流式响应体要等异步结果被取回才会落到 response 里，直接读会拿到空串。
     */
    private void assertStreamsAnswer(RequestBuilder builder) throws Exception {
        MvcResult result = this.mockMvc.perform(builder).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).contains("text/event-stream");
        assertThat(result.getRequest().isAsyncStarted()).isTrue();

        result.getAsyncResult(5000);

        assertThat(result.getResponse().getContentAsString()).startsWith("data:").contains("answer-");
    }

    private void chat(String prompt, String chatId) throws Exception {
        assertStreamsAnswer(get("/ai/chat").param("prompt", prompt).param("chatId", chatId));
    }

    private java.util.List<String> texts(String chatId) {
        return this.chatMemory.get(chatId).stream().map(MessageVO::new).map(MessageVO::getContent).toList();
    }

}
