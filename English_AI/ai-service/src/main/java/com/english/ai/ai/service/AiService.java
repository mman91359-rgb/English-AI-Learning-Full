package com.english.ai.ai.service;

import com.english.ai.ai.dto.ChatRequest;
import com.english.ai.ai.dto.WordDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AiService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.base-url}")
    private String baseUrl;

    @Value("${dashscope.model}")
    private String model;

    @Value("${dashscope.max-tokens}")
    private int maxTokens;

    @Value("${dashscope.temperature}")
    private double temperature;

    private static final Map<String, String> SYSTEM_PROMPTS = new HashMap<>();
    static {
        SYSTEM_PROMPTS.put("vocab", "你是一位帮助中国大学生备考四六级的英语老师。当前学生在做词汇填空题。\n\n" +
            "重要规则：\n" +
            "1. 必须解释【核心单词】字段中的英文单词，不要解释中文！\n" +
            "2. 音标必须使用国际音标IPA格式，用斜杠包裹，如 /ˈsɜːkəmstəns/，禁止用中文拼音或谐音！\n" +
            "3. 严格按照以下结构化Markdown格式回答，每个##标题占一行，内容另起一行：\n\n" +
            "## 📖 单词详解\n" +
            "**单词**：单词原形\n" +
            "**音标**：/ˈɪntərnæʃənl/\n" +
            "**词性**：n./v./adj.\n\n" +
            "## 💡 核心释义\n" +
            "1. 第一条释义\n" +
            "2. 第二条释义\n\n" +
            "## 📝 常用搭配\n" +
            "1. 搭配短语1\n" +
            "2. 搭配短语2\n\n" +
            "## ⚠️ 用法要点\n" +
            "- 注意事项1\n" +
            "- 注意事项2\n\n" +
            "## 🌰 例句\n" +
            "英文例句（中文翻译）\n\n" +
            "回答要简洁清晰，不超过250字。确保英文单词之间有正确空格。");
        SYSTEM_PROMPTS.put("grammar", "你是一位帮助中国大学生备考四六级的英语老师。当前学生在做语法连词成句题。\n\n" +
            "重要规则：严格按照以下结构化Markdown格式回答，每个##标题占一行，内容另起一行：\n\n" +
            "## 📖 语法知识点\n" +
            "知识点名称\n\n" +
            "## 💡 核心规则\n" +
            "1. 规则说明1\n" +
            "2. 规则说明2\n\n" +
            "## 🔍 句子解析\n" +
            "详细分析句子结构和成分\n\n" +
            "## ⚠️ 易错点\n" +
            "- 常见错误1\n" +
            "- 常见错误2\n\n" +
            "## 🌰 例句\n" +
            "英文例句（中文翻译）\n\n" +
            "回答要简洁清晰，不超过250字。确保英文单词之间有正确空格。");
        SYSTEM_PROMPTS.put("general", "你是一位帮助中国大学生备考四六级的英语老师。请用中文回答学生的英语学习问题。回答必须使用结构化格式，包含二级标题（##）和有序/无序列表，结构清晰、逻辑分明，注意单词间保留正确空格。");
        SYSTEM_PROMPTS.put("speaking", "你是一位英语口语考官。请根据学生的口语内容进行评分（满分100分），从发音清晰度、语法准确性、流利度、词汇丰富度四个维度打分，最后给出总分和改进建议。用中文回答。");
        SYSTEM_PROMPTS.put("writing", "你是一位英语写作考官。请根据学生的作文内容进行评分（满分100分），从语法、词汇、连贯性、内容深度四个维度打分，最后给出总分和修改建议。用中文回答，并给出改进后的范文片段。");
        // 单词详情生成：要求模型严格输出 JSON，便于后端解析
        SYSTEM_PROMPTS.put("word_detail",
            "你是一位英语词典编辑。请根据用户给出的英文单词，生成结构化的词典信息。" +
            "必须严格返回如下 JSON 格式（不要包含任何额外文字、不要使用 markdown 代码块标记）：\n" +
            "{\"word\":\"单词原形\",\"phonetic\":\"音标含斜杠，如 /ˈsɜːkəmstəns/\",\"pos\":\"主要词性，如 n./v./adj.，多个用/分隔\"," +
            "\"translation\":\"中文释义，多条用；分隔\",\"example\":\"一句简短英文例句\"," +
            "\"exampleTranslation\":\"例句中文翻译\",\"collocation\":\"常见搭配，多个用；分隔\"}\n" +
            "要求：音标使用国际音标；例句难度适合四六级；释义和例句要准确；JSON 字段名必须完全一致。"
        );
    }

    /**
     * 同步生成单词详情（非流式）。
     * 调用 AI 接口获取音标、词性、释义、例句、搭配等结构化信息。
     * 用于"单词本智能化"场景：用户只输入单词，AI 自动补全其余字段。
     *
     * @param word 英文单词
     * @return 单词详情；AI 不可用或解析失败时返回 null
     */
    public WordDetailVO generateWordDetail(String word) {
        if (word == null || word.isBlank()) return null;
        String w = word.trim();

        // 未配置 API Key，走 Mock 兜底
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI API Key 未配置，使用 Mock 单词详情: {}", w);
            return mockWordDetail(w);
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPTS.get("word_detail")));
            messages.add(Map.of("role", "user", "content", "请生成单词 \"" + w + "\" 的词典信息，严格按指定 JSON 格式返回。"));

            String requestBody = buildRequestBodySync(messages);
            HttpURLConnection conn = createConnectionSync(requestBody);
            int code = conn.getResponseCode();
            if (code != 200) {
                String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                log.error("调用AI API生成单词详情失败: {} {}，使用 Mock 兜底", code, err);
                return mockWordDetail(w);
            }

            // 非流式响应：一次性读取完整 JSON
            String respBody;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                respBody = sb.toString();
            }
            String content = extractContentFromSync(respBody);
            if (content == null || content.isBlank()) {
                log.warn("AI 返回内容为空，使用 Mock 兜底: {}", w);
                return mockWordDetail(w);
            }
            WordDetailVO vo = parseWordDetailJson(content, w);
            if (vo == null) {
                log.warn("AI 返回内容解析失败，使用 Mock 兜底: {} -> {}", w, content);
                return mockWordDetail(w);
            }
            return vo;
        } catch (Exception e) {
            log.error("生成单词详情异常: {}", w, e);
            return mockWordDetail(w);
        }
    }

    /**
     * Mock 兜底：API 不可用时返回最简信息
     */
    private WordDetailVO mockWordDetail(String word) {
        WordDetailVO vo = new WordDetailVO();
        vo.setWord(word);
        vo.setPhonetic("");
        vo.setPos("");
        vo.setTranslation("（AI 服务不可用，请稍后手动补充释义）");
        vo.setExample("");
        vo.setExampleTranslation("");
        vo.setCollocation("");
        return vo;
    }

    /**
     * 解析 AI 返回的 JSON 文本为 WordDetailVO。
     * 兼容模型可能多输出的 ```json ``` 包裹或前后多余文字。
     */
    private WordDetailVO parseWordDetailJson(String content, String fallbackWord) {
        if (content == null) return null;
        // 去除可能的 markdown 代码块标记
        String s = content.trim();
        if (s.startsWith("```")) {
            int start = s.indexOf('\n');
            int end = s.lastIndexOf("```");
            if (start > 0 && end > start) s = s.substring(start + 1, end).trim();
        }
        // 截取第一个 { 到最后一个 }
        int li = s.indexOf('{');
        int ri = s.lastIndexOf('}');
        if (li < 0 || ri <= li) return null;
        String json = s.substring(li, ri + 1);

        WordDetailVO vo = new WordDetailVO();
        vo.setWord(extractJsonString(json, "word", fallbackWord));
        vo.setPhonetic(extractJsonString(json, "phonetic", ""));
        vo.setPos(extractJsonString(json, "pos", ""));
        vo.setTranslation(extractJsonString(json, "translation", ""));
        vo.setExample(extractJsonString(json, "example", ""));
        vo.setExampleTranslation(extractJsonString(json, "exampleTranslation", ""));
        vo.setCollocation(extractJsonString(json, "collocation", ""));
        return vo;
    }

    /**
     * 从 JSON 文本中按字段名提取字符串值（简易解析，避免引入 Jackson 依赖）。
     * 支持 "key":"value" 与 "key": "value" 两种格式。
     */
    private String extractJsonString(String json, String key, String def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher m = p.matcher(json);
        if (!m.find()) return def;
        String val = m.group(1);
        // 反转义常见字符
        return val.replace("\\\"", "\"")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\t", "\t");
    }

    /**
     * 从非流式响应体中提取 content 字段
     * 响应格式：{"choices":[{"message":{"content":"..."}}]}
     */
    private String extractContentFromSync(String respBody) {
        if (respBody == null) return null;
        return extractJsonString(respBody, "content", null);
    }

    /**
     * 构建非流式请求体（stream=false）
     */
    private String buildRequestBodySync(List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(model).append("\",");
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, String> msg = messages.get(i);
            sb.append("{\"role\":\"").append(msg.get("role")).append("\",");
            sb.append("\"content\":\"").append(escapeJson(msg.get("content"))).append("\"}");
        }
        sb.append("],");
        sb.append("\"max_tokens\":").append(maxTokens).append(",");
        sb.append("\"temperature\":").append(temperature).append(",");
        sb.append("\"stream\":false");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 创建非流式 HTTP 连接
     */
    private HttpURLConnection createConnectionSync(String body) throws Exception {
        URI uri = URI.create(baseUrl + "/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        return conn;
    }

    public SseEmitter chat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2分钟超时

        CompletableFuture.runAsync(() -> {
            try {
                String systemPrompt = SYSTEM_PROMPTS.getOrDefault(request.getContextType(), SYSTEM_PROMPTS.get("general"));
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", systemPrompt));

                // 附加上下文
                StringBuilder userMsg = new StringBuilder();
                if (request.getQuestionContent() != null && !request.getQuestionContent().isBlank()) {
                    userMsg.append("【当前题目】").append(request.getQuestionContent()).append("\n");
                }
                if (request.getUserAnswer() != null && !request.getUserAnswer().isBlank()) {
                    userMsg.append("【我的答案】").append(request.getUserAnswer()).append("\n");
                }
                userMsg.append(request.getMessage());
                messages.add(Map.of("role", "user", "content", userMsg.toString()));

                // 如果没有配置有效 API Key，使用 Mock 模式
                if (apiKey == null || apiKey.isBlank()) {
                    sendMockReply(request.getContextType(), userMsg.toString(), emitter);
                    return;
                }

                String requestBody = buildRequestBody(messages);
                HttpURLConnection conn = createConnection(requestBody);
                int code = conn.getResponseCode();
                if (code != 200) {
                    String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("调用AI API失败: {} {}，切换到Mock模式", code, err);
                    // API 调用失败，降级到 Mock 模式
                    sendMockReply(request.getContextType(), userMsg.toString(), emitter);
                    return;
                }

                // 读取流式响应
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder contentBuilder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) break;
                            try {
                                String content = extractContent(data);
                                if (content != null) {
                                    contentBuilder.append(content);
                                    emitter.send(SseEmitter.event().name("token").data(content));
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    emitter.send(SseEmitter.event().name("done").data(contentBuilder.toString()));
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("SSE流式响应异常，切换到Mock模式", e);
                // 异常时也尝试用 Mock 兜底
                try {
                    sendMockReply(request.getContextType(), request.getMessage(), emitter);
                } catch (Exception ex) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data("连接中断：" + ex.getMessage()));
                    } catch (Exception ignored) {}
                    emitter.completeWithError(ex);
                }
            }
        });

        emitter.onTimeout(() -> {
            log.warn("SSE连接超时");
            emitter.complete();
        });
        emitter.onError(e -> emitter.completeWithError(e));

        return emitter;
    }

    /**
     * Mock 模式：根据上下文类型生成模拟回复，流式输出
     */
    private void sendMockReply(String contextType, String userMsg, SseEmitter emitter) throws Exception {
        String reply = generateMockReply(contextType, userMsg);
        // 模拟流式输出，每个字 30ms 延迟
        for (int i = 0; i < reply.length(); i++) {
            String ch = String.valueOf(reply.charAt(i));
            emitter.send(SseEmitter.event().name("token").data(ch));
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        emitter.send(SseEmitter.event().name("done").data(reply));
        emitter.complete();
    }

    private String generateMockReply(String contextType, String userMsg) {
        String msg = userMsg == null ? "" : userMsg.toLowerCase();
        switch (contextType == null ? "general" : contextType) {
            case "vocab":
                if (msg.contains("account")) {
                    return "【单词解析】account\n\n" +
                           "📖 含义：\n" +
                           "• 名词：描述；叙述；账户\n" +
                           "• 动词：解释；说明\n\n" +
                           "🔑 固定搭配：\n" +
                           "• give/provide an account of 描述\n" +
                           "• a detailed account of 详细描述\n" +
                           "• on account of 因为；由于\n" +
                           "• take...into account 考虑到\n\n" +
                           "📝 例句：\n" +
                           "The book gives a detailed account of the event.\n" +
                           "这本书详细描述了这一事件。\n\n" +
                           "💡 注意区分：\n" +
                           "• account（描述/账户）vs description（描述）\n" +
                           "  account 更强调叙事性、来龙去脉；\n" +
                           "  description 更强调客观、静态的描写。";
                }
                if (msg.contains("produce") || msg.contains("product")) {
                    return "【易混词辨析】produce vs product\n\n" +
                           "🔹 produce /prəˈdjuːs/\n" +
                           "• 动词：生产；制造；产生\n" +
                           "• 名词：农产品（不可数）\n" +
                           "• 例句：The factory produces cars.\n\n" +
                           "🔹 product /ˈprɒdʌkt/\n" +
                           "• 名词：产品；产物（可数）\n" +
                           "• 例句：This is our new product.\n\n" +
                           "💡 记忆技巧：\n" +
                           "produce 是动作（生产），product 是结果（产品）\n" +
                           "类似：decide → decision";
                }
                return "📚 词汇学习小贴士：\n\n" +
                       "1️⃣ 词根词缀法：记住常见词根，可以快速扩展词汇量\n" +
                       "2️⃣ 语境记忆法：在句子和文章中记单词，更牢固\n" +
                       "3️⃣ 间隔重复法：用艾宾浩斯遗忘曲线安排复习\n" +
                       "4️⃣ 联想记忆法：把新词和已知词联系起来\n\n" +
                       "💡 有具体单词想了解的话，直接告诉我！";
            case "grammar":
                if (msg.contains("wish") || msg.contains("虚拟")) {
                    return "【语法解析】wish 虚拟语气\n\n" +
                           "📌 与现在事实相反：\n" +
                           "   主句 + wish + 主语 + 过去式（be用were）\n" +
                           "   例：I wish I had more time.\n\n" +
                           "📌 与过去事实相反：\n" +
                           "   主句 + wish + 主语 + had + 过去分词\n" +
                           "   例：I wish I had studied harder in college.\n" +
                           "   （我真希望大学时能更努力学习。）\n\n" +
                           "📌 与将来事实相反：\n" +
                           "   主句 + wish + 主语 + would/could + 动词原形\n" +
                           "   例：I wish it would rain tomorrow.\n\n" +
                           "💡 口诀：退一步法\n" +
                           "现在 → 过去，过去 → 过去完成，将来 → would+动词";
                }
                if (msg.contains("only one") || msg.contains("主谓一致")) {
                    return "【语法解析】the only one of 主谓一致\n\n" +
                           "📌 the only one of + 复数名词 + who/that + 单数谓语\n" +
                           "   → 先行词是 the only one（单数）\n" +
                           "   例：He is the only one of the students who has passed the test.\n" +
                           "       （那些学生中只有他通过了考试。）\n\n" +
                           "📌 one of + 复数名词 + who/that + 复数谓语\n" +
                           "   → 先行词是复数名词\n" +
                           "   例：He is one of the students who have passed the test.\n" +
                           "       （他是通过考试的学生之一。）\n\n" +
                           "💡 关键区别：\n" +
                           "有 the only → 单数谓语\n" +
                           "没有 the only → 复数谓语";
                }
                return "📖 语法知识点解析：\n\n" +
                       "这是一个很好的语法问题！\n\n" +
                       "💡 学习建议：\n" +
                       "1. 先掌握基本概念和定义\n" +
                       "2. 通过例句理解用法\n" +
                       "3. 多做练习巩固\n" +
                       "4. 总结易错点\n\n" +
                       "告诉我具体的语法点，我来详细讲解！";
            case "writing":
                return "✍️ 作文评分与建议：\n\n" +
                       "【评分】82/100\n\n" +
                       "📊 各维度得分：\n" +
                       "• 语法：20/25 （少量时态错误）\n" +
                       "• 词汇：21/25 （用词较基础，可增加高级词汇）\n" +
                       "• 连贯性：22/25 （逻辑清晰，过渡自然）\n" +
                       "• 内容深度：19/25 （观点明确，论据可更充实）\n\n" +
                       "💡 修改建议：\n" +
                       "1. 多用复杂句型（定语从句、倒装句）\n" +
                       "2. 增加高级词汇替换常见词\n" +
                       "3. 每个论点配一个具体例子\n" +
                       "4. 注意主谓一致和时态统一\n\n" +
                       "把你的作文发给我，我来详细点评！";
            case "speaking":
                return "🎤 口语评分：85/100\n\n" +
                       "📊 各维度得分：\n" +
                       "• 发音清晰度：22/25\n" +
                       "• 语法准确性：20/25\n" +
                       "• 流利度：23/25\n" +
                       "• 词汇丰富度：20/25\n\n" +
                       "💡 改进建议：\n" +
                       "1. 注意第三人称单数动词加 s\n" +
                       "2. 增加话题词汇积累\n" +
                       "3. 多做影子跟读法练习发音\n" +
                       "4. 用录音自查，发现自己的问题\n\n" +
                       "继续加油！坚持练习一定会进步！";
            default:
                return "👋 你好！我是你的英语学习助手。\n\n" +
                       "我可以帮你：\n" +
                       "📖 解释单词含义和用法\n" +
                       "📚 讲解语法知识点\n" +
                       "✍️ 批改作文和翻译\n" +
                       "🎤 练习口语和听力\n" +
                       "💡 提供学习方法建议\n\n" +
                       "有什么英语问题，随时问我吧！";
        }
    }

    private String extractContent(String jsonData) {
        // 解析 {"choices":[{"delta":{"content":"Hello"},"index":0}]}
        int contentIdx = jsonData.indexOf("\"content\":\"");
        if (contentIdx < 0) return null;
        int start = contentIdx + 11;
        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        for (int i = start; i < jsonData.length(); i++) {
            char c = jsonData.charAt(i);
            if (escaping) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(c); break;
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String buildRequestBody(List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(model).append("\",");
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, String> msg = messages.get(i);
            sb.append("{\"role\":\"").append(msg.get("role")).append("\",");
            sb.append("\"content\":\"").append(escapeJson(msg.get("content"))).append("\"}");
        }
        sb.append("],");
        sb.append("\"max_tokens\":").append(maxTokens).append(",");
        sb.append("\"temperature\":").append(temperature).append(",");
        sb.append("\"stream\":true,");
        sb.append("\"stream_options\":{\"include_usage\":true}");
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 语义可接受性判对（同步，非流式）
     * 由 exercise-service 判题引擎 Level 2.5 调用。
     * 判断用户答案是否语义可接受（近义词、同义表达等），
     * 返回结构化 JSON，供 exercise-service 判断是否给分。
     */
    public Map<String, Object> judgeAcceptable(String userAnswer, String correctAnswer, String context) {
        Map<String, Object> result = new HashMap<>();
        result.put("acceptable", false);
        result.put("reason", "");
        result.put("altAnswer", "");

        if (apiKey == null || apiKey.isBlank()) {
            // 无 API Key 时本地规则兜底：只做简单的同义词表匹配
            String localResult = checkLocalSynonyms(userAnswer, correctAnswer);
            if (localResult != null) {
                result.put("acceptable", true);
                result.put("reason", "近义词/同义表达，语义一致");
                result.put("altAnswer", userAnswer.trim().toLowerCase());
            } else {
                result.put("reason", "AI 服务不可用，无法进行语义判对，请使用标准答案");
            }
            return result;
        }

        try {
            String prompt = buildAcceptablePrompt(userAnswer, correctAnswer, context);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                "你是一位英语判题助手。请判断学生的答案在语义上是否可以接受（即使不完全等于标准答案）。" +
                "只返回 JSON 格式：{\"acceptable\":true/false,\"reason\":\"简短中文理由\",\"altAnswer\":\"可接受的替代表达\"}。" +
                "不要返回任何其他文字。若语义可接受，altAnswer 为用户的答案原文；若不可接受，altAnswer 为空。"));
            messages.add(Map.of("role", "user", "content", prompt));

            String requestBody = buildRequestBodySync(messages);
            HttpURLConnection conn = createConnectionSync(requestBody);
            int code = conn.getResponseCode();
            if (code != 200) {
                log.warn("语义判对AI调用失败: {}，使用本地规则兜底", code);
                String localResult = checkLocalSynonyms(userAnswer, correctAnswer);
                if (localResult != null) {
                    result.put("acceptable", true);
                    result.put("reason", "近义词/同义表达，语义一致");
                    result.put("altAnswer", userAnswer.trim().toLowerCase());
                }
                return result;
            }

            String respBody;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                respBody = sb.toString();
            }
            String aiContent = extractContentFromSync(respBody);
            if (aiContent != null && !aiContent.isBlank()) {
                // 解析 AI 返回的 JSON
                String json = aiContent.trim();
                if (json.startsWith("```")) {
                    int start = json.indexOf('\n');
                    int end = json.lastIndexOf("```");
                    if (start > 0 && end > start) json = json.substring(start + 1, end).trim();
                }
                int li = json.indexOf('{');
                int ri = json.lastIndexOf('}');
                if (li >= 0 && ri > li) {
                    json = json.substring(li, ri + 1);
                }
                Boolean acceptable = parseJsonBoolean(json, "acceptable");
                String reason = extractJsonString(json, "reason", "");
                String altAnswer = extractJsonString(json, "altAnswer", "");

                if (acceptable != null) {
                    result.put("acceptable", acceptable);
                    result.put("reason", reason);
                    result.put("altAnswer", altAnswer);
                }
            }
        } catch (Exception e) {
            log.warn("语义判对AI异常: {}，使用本地规则兜底", e.getMessage());
            String localResult = checkLocalSynonyms(userAnswer, correctAnswer);
            if (localResult != null) {
                result.put("acceptable", true);
                result.put("reason", "近义词/同义表达，语义一致");
                result.put("altAnswer", userAnswer.trim().toLowerCase());
            }
        }
        return result;
    }

    /**
     * 本地同义词兜底表：当 AI 不可用时，做简单的同义词匹配
     * 返回匹配到的同义词组，未匹配返回 null
     */
    private String checkLocalSynonyms(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) return null;
        String ua = userAnswer.trim().toLowerCase();
        String ca = correctAnswer.trim().toLowerCase();
        if (ua.equals(ca)) return ua;

        // 常见四六级近义词/同义表达对
        String[][] synonymPairs = {
            {"cause", "reason", "factor"},
            {"effect", "impact", "influence", "consequence"},
            {"important", "significant", "crucial", "essential", "vital"},
            {"show", "demonstrate", "indicate", "reveal", "illustrate"},
            {"problem", "issue", "challenge", "difficulty"},
            {"improve", "enhance", "boost", "strengthen"},
            {"reduce", "decrease", "lower", "diminish"},
            {"increase", "rise", "grow", "expand"},
            {"believe", "think", "consider", "argue", "maintain"},
            {"many", "numerous", "various", "multiple"},
            {"big", "large", "huge", "enormous", "vast"},
            {"small", "tiny", "minor", "slight"},
            {"good", "excellent", "outstanding", "remarkable"},
            {"bad", "poor", "terrible", "awful"},
            {"help", "assist", "aid", "support"},
            {"need", "require", "demand", "necessitate"},
            {"use", "utilize", "employ", "apply"},
            {"get", "obtain", "acquire", "gain"},
            {"make", "create", "produce", "generate"},
            {"circumstance", "situation", "condition", "context"},
            {"opportunity", "chance", "possibility"},
            {"result", "outcome", "consequence"},
            {"method", "approach", "way", "means"},
            {"benefit", "advantage", "merit"},
            {"drawback", "disadvantage", "shortcoming"},
        };

        for (String[] group : synonymPairs) {
            boolean uaInGroup = false;
            boolean caInGroup = false;
            for (String s : group) {
                if (s.equals(ua)) uaInGroup = true;
                if (s.equals(ca)) caInGroup = true;
            }
            if (uaInGroup && caInGroup) return ua;
        }
        return null;
    }

    /**
     * 从 JSON 中提取布尔值
     */
    private Boolean parseJsonBoolean(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return "true".equals(m.group(1));
        }
        return null;
    }

    private String buildAcceptablePrompt(String userAnswer, String correctAnswer, String context) {
        return String.format(
            "题干：%s\n标准答案：%s\n学生答案：%s\n\n" +
            "请判断学生答案在语义上是否可以接受（近义词、同义表达、不同但合理的填词等）。" +
            "只返回 JSON，不要其他文字。",
            context != null ? context : "",
            correctAnswer != null ? correctAnswer : "",
            userAnswer != null ? userAnswer : "");
    }

    /**
     * 判题反馈：根据用户错误答案生成启发式提示（同步，非流式）
     * 由 exercise-service 的判题引擎 Level 3 调用
     */
    public Map<String, Object> judgeFeedback(String userAnswer, String correctAnswer, String context) {
        Map<String, Object> result = new HashMap<>();
        result.put("errorType", "UNKNOWN");

        if (apiKey == null || apiKey.isBlank()) {
            String hint = generateJudgeHint(userAnswer, correctAnswer, context);
            result.put("hint", hint);
            result.put("suggestions", List.of(hint));
            return result;
        }

        try {
            String prompt = buildJudgePrompt(userAnswer, correctAnswer, context);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                "你是一位英语判题助手。根据用户错误答案和正确答案，用中文给出简短启发式提示（30字以内），引导学生思考。只返回提示文本，不要任何额外内容。"));
            messages.add(Map.of("role", "user", "content", prompt));

            String requestBody = buildRequestBodySync(messages);
            HttpURLConnection conn = createConnectionSync(requestBody);
            int code = conn.getResponseCode();
            if (code != 200) {
                log.warn("判题反馈AI调用失败: {}，使用本地规则", code);
                String hint = generateJudgeHint(userAnswer, correctAnswer, context);
                result.put("hint", hint);
                result.put("suggestions", List.of(hint));
                return result;
            }

            String respBody;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                respBody = sb.toString();
            }
            String aiHint = extractContentFromSync(respBody);
            if (aiHint != null && !aiHint.isBlank()) {
                result.put("hint", aiHint);
                result.put("suggestions", List.of(aiHint));
            } else {
                String hint = generateJudgeHint(userAnswer, correctAnswer, context);
                result.put("hint", hint);
                result.put("suggestions", List.of(hint));
            }
        } catch (Exception e) {
            log.warn("判题反馈AI异常: {}，使用本地规则", e.getMessage());
            String hint = generateJudgeHint(userAnswer, correctAnswer, context);
            result.put("hint", hint);
            result.put("suggestions", List.of(hint));
        }
        return result;
    }

    private String buildJudgePrompt(String userAnswer, String correctAnswer, String context) {
        return String.format(
            "用户答案：%s\n正确答案：%s\n题干上下文：%s\n请分析用户错误原因并给出简短启发提示。",
            userAnswer != null ? userAnswer : "",
            correctAnswer != null ? correctAnswer : "",
            context != null ? context : "");
    }

    private String generateJudgeHint(String userAnswer, String correctAnswer, String context) {
        if (userAnswer == null || correctAnswer == null) return "请检查你的答案，再试一次。";
        String ua = userAnswer.trim().toLowerCase();
        String ca = correctAnswer.trim().toLowerCase();

        if (ua.length() < ca.length() && ca.contains(ua)) {
            return "答案不完整，句子还缺少一些词，仔细看看题干。";
        }
        if (ua.length() > ca.length() * 1.5) {
            return "答案太长了，去掉多余的词再试试。";
        }
        int commonWords = 0;
        String[] uaWords = ua.split("\\s+");
        String[] caWords = ca.split("\\s+");
        for (String uw : uaWords) {
            for (String cw : caWords) {
                if (uw.equals(cw)) { commonWords++; break; }
            }
        }
        if (commonWords > 0 && commonWords < caWords.length) {
            return "你选的词大部分是对的，但词序需要调整。";
        }
        return "这个答案不太对哦，再想一想正确的表达方式。";
    }

    /**
     * AI 批量生成题目（同步，非流式）
     * 按知识点 + 难度 + 等级生成指定数量的题目
     * 返回 JSON 数组字符串，每道题包含 stem/options/correctAnswer/analysis
     *
     * @param knowledgePoint 知识点名称
     * @param cetLevel       目标等级: CET4/CET6
     * @param difficulty     难度等级: 1~8
     * @param questionType   题型: FILL_BLANK/SENTENCE_MAKING
     * @param count          生成题目数量（上限20）
     * @return AI 生成的题目 JSON 数组字符串，失败时返回 "[]"
     */
    public String generateQuestions(String knowledgePoint, String cetLevel, int difficulty,
                                     String questionType, int count) {
        // 未配置 API Key，返回空数组
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI API Key 未配置，无法生成题目");
            return "[]";
        }
        try {
            // 构建出题 prompt
            String prompt = String.format(
                "请生成 %d 道英语%s练习题，知识点：%s，难度1-8为%d，题型：%s。\n" +
                "每道题用 JSON 格式返回，包含：stem(题干，用___表示填空位)、" +
                "options(选项JSON数组，4个选项)、correctAnswer(正确答案)、analysis(中文解析)。\n" +
                "只返回一个 JSON 数组，不要任何其他文字。",
                count, "CET4".equals(cetLevel) ? "四级" : "六级",
                knowledgePoint, difficulty,
                "FILL_BLANK".equals(questionType) ? "词汇填空" : "语法连词成句");

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                "你是一位英语四六级出题专家。请根据要求生成高质量练习题。只返回JSON数组，不要markdown代码块标记，不要任何额外文字。"));
            messages.add(Map.of("role", "user", "content", prompt));

            // 调用 AI 接口（非流式）
            String requestBody = buildRequestBodySync(messages);
            HttpURLConnection conn = createConnectionSync(requestBody);
            int code = conn.getResponseCode();
            if (code != 200) {
                log.error("AI生成题目失败: {}", code);
                return "[]";
            }
            // 读取响应体
            String respBody;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                respBody = sb.toString();
            }
            // 提取 content 字段
            String content = extractContentFromSync(respBody);
            if (content != null && !content.isBlank()) {
                // 清理可能的 markdown 代码块标记
                content = content.trim();
                if (content.startsWith("```")) {
                    int start = content.indexOf('\n');
                    int end = content.lastIndexOf("```");
                    if (start > 0 && end > start) content = content.substring(start + 1, end).trim();
                }
                return content;
            }
        } catch (Exception e) {
            log.error("AI生成题目异常: {}", e.getMessage());
        }
        return "[]";
    }

    private HttpURLConnection createConnection(String body) throws Exception {
        URI uri = URI.create(baseUrl + "/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(90_000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        return conn;
    }
}
