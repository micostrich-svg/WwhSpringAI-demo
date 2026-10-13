# Spring AI Demo

一个基于 Spring Boot 4.1 与 Spring AI 2.0 的最小聊天服务：把 DeepSeek（OpenAI 兼容接口）接入 `ChatClient`，
支持流式回答与服务端会话记忆。

## 运行前配置

API Key 放在 **`src/main/resources/application-dev.yaml`**（该文件已在 `.gitignore` 里，不会提交）。
默认 profile 就是 `dev`，所以启动即生效。两种给 key 的方式：

**方式一（推荐）：设环境变量，文件不动**

```powershell
$env:AI_API_KEY = "sk-..."     # 只对当前窗口有效；永久生效用 setx AI_API_KEY "sk-..."
./mvnw spring-boot:run
```

IDEA 里则在 Run/Debug Configurations → Environment variables 加一条 `AI_API_KEY=sk-...`。

**方式二：直接把 key 写在 `application-dev.yaml` 里**

```yaml
api-key: sk-xxxxxxxxxxxxxxxx
```

> ⚠️ 两种都没有时程序**不会启动报错**，但请求会带空凭证，DeepSeek 返回 `401 Unauthorized`
> （日志里是 `com.openai.errors.UnauthorizedException: 401`）——遇到 401 先查这里。

模型名等其他参数也在同一个文件：

```yaml
spring:
  ai:
    openai:
      base-url: https://api.deepseek.com
      api-key: ${AI_API_KEY:}
      chat:
        options:
          model: ${AI_MODEL:deepseek-chat}   # 你的环境若用别的模型名，改这里
          temperature: 0.8
```

## 接口

| 方法 | 提交方式 | 说明 |
| --- | --- | --- |
| GET | `/ai/chat?prompt=...&chatId=...` | 浏览器/curl 直连 |
| POST | `multipart/form-data`（字段 `prompt`、`chatId`） | 浏览器 `FormData` |
| POST | `application/x-www-form-urlencoded`（字段 `prompt`、`chatId`） | 表单提交 |
| GET | `/ai/history/{type}` | 列出某个类型下已有的会话 id，例如 `/ai/history/chat` |
| GET | `/ai/history/{type}/{chatId}` | 返回该会话已记住的消息（`role` + `content`），最旧的在前 |

`/ai/chat` 的 `prompt` 省略时默认 `讲个笑话`；`chatId` 必填，缺了返回 400。

三种提交方式返回的都是同样的 SSE 流：每个输出块一行 `data:`，块内容里不含换行，
所以前端只要按行取 `data:` 后面的内容即可（**不要对正文做 `trim()`**，会把模型输出里的空格吃掉）：

```js
// FormData 提交
const form = new FormData();
form.append("prompt", prompt);
form.append("chatId", chatId);
const res = await fetch("/ai/chat", { method: "POST", body: form }); // 不要手动设置 Content-Type

const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();
for (let chunk; !(chunk = await reader.read()).done; ) {
  for (const line of chunk.value.split("\n")) {
    if (line.startsWith("data:")) append(line.slice(5));
  }
}
```

```powershell
curl.exe -N -X POST http://localhost:8080/ai/chat -F "prompt=你好" -F "chatId=abc"
curl.exe -N "http://localhost:8080/ai/chat?prompt=你好&chatId=abc"
curl.exe "http://localhost:8080/ai/history/chat"
curl.exe "http://localhost:8080/ai/history/chat/abc"
```

## 会话记忆

- 记忆由 Spring AI 自动配置的 `ChatMemory`（`MessageWindowChatMemory`）提供，默认保留最近 20 条消息，
  窗口大小在 `CommonConfiguration` 中调整。
- 每次请求通过 `ChatMemory.CONVERSATION_ID` 把 `chatId` 传给 `MessageChatMemoryAdvisor`，
  所以对话是按 `chatId` 隔离的。
- **`chatId` 必须由前端为同一段对话保持稳定**。如果每次请求都生成新的 `chatId`（比如用时间戳），
  服务端会认为这是新会话，上一轮的消息就不会被带进上下文——这是“记忆不生效”最常见的原因。
  排查方法：看 debug 日志里 `MessageChatMemoryAdvisor` 收到的 conversation id 是否一致。
- 消息在每个回合发生时立刻写入记忆（用户消息在请求前、助手回复在流结束后），
  所以客户端中途断开不会丢掉这一轮；请求一被接受就会出现在 `/ai/history/{type}` 列表里。
- `ChatHistoryRepository` 的实现只是进程内 `Map`，**重启进程即清空**，多实例部署也不共享；
  需要持久化时替换该实现（或改用 Spring AI 的 JDBC 记忆仓储）。
- `MessageWindowChatMemory` 默认保留最近 20 条消息，窗口大小在 `CommonConfiguration` 里调整。

## 构建与测试

```powershell
./mvnw test
```

`ChatMemoryIntegrationTests` 和 `ChatApiTests` 用本地的假模型（`StubChatModel`）跑完整链路：
GET/multipart 的 SSE 输出、参数校验、会话 id 是否传到记忆顾问、多轮对话是否按会话隔离、
来回切换会话后消息是否还在，因此不需要网络或真实 API Key。

## 参考文档

- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)
- [Spring Boot 4.1 Reference](https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html)
- [DeepSeek API](https://api-docs.deepseek.com/)
