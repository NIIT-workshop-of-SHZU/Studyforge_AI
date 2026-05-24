package com.studyforge.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.ai.service.AiService;
import com.studyforge.system.service.IntegrationSettingService;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SiliconFlowAiServiceImpl implements AiService {
    private static final String DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1";
    private static final String DEFAULT_MODEL = "deepseek-ai/DeepSeek-V4-Flash";
    private static final Duration AI_TIMEOUT = Duration.ofSeconds(200);

    private final IntegrationSettingService integrationSettingService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final LocalFallbackAiServiceImpl fallback = new LocalFallbackAiServiceImpl();

    public SiliconFlowAiServiceImpl(IntegrationSettingService integrationSettingService) {
        this.integrationSettingService = integrationSettingService;
        this.objectMapper = new ObjectMapper();
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(AI_TIMEOUT);
        proxySelector().ifPresent(builder::proxy);
        this.httpClient = builder.build();
    }

    @Override
    public String generateSummary(String content, String language) {
        return complete("""
                请用用户指定语言提炼这篇学习内容。输出要像真实学习产品里的 AI 摘要：
                1. 先给 3 条要点；
                2. 再给 1 句适合收藏的结论；
                3. 不要提“根据文档/作为 AI”。
                语言：%s
                内容：
                %s
                """.formatted(language, content), () -> fallback.generateSummary(content, language));
    }

    @Override
    public String generateTags(String content, String language) {
        return complete("为下面内容生成 4 到 6 个学习标签，用逗号分隔。语言：" + language + "\n" + content,
                () -> fallback.generateTags(content, language));
    }

    @Override
    public String recommendCategory(String content, String language) {
        return complete("从 TECHNOLOGY、BUSINESS、PRODUCTIVITY、CAREER、FINANCE 中选一个最适合的分类，只输出分类编码。\n" + content,
                () -> fallback.recommendCategory(content, language));
    }

    @Override
    public String translateText(String text, String sourceLang, String targetLang) {
        return complete("把下面文本从 " + sourceLang + " 翻译到 " + targetLang + "，只输出译文。\n" + text,
                () -> fallback.translateText(text, sourceLang, targetLang));
    }

    @Override
    public String moderateContent(String content, String language) {
        return complete("判断下面内容是否适合学习社区发布，输出 LOW_RISK、MEDIUM_RISK 或 HIGH_RISK，并给一句理由。\n" + content,
                () -> fallback.moderateContent(content, language));
    }

    @Override
    public String answerQuestion(String postContent, String question, String answerLanguage) {
        return complete("""
                你是 StudyForge AI 的学习助手。请只依据文章内容回答问题。
                回答语言：%s
                文章：
                %s
                问题：
                %s
                """.formatted(answerLanguage, postContent, question), () -> fallback.answerQuestion(postContent, question, answerLanguage));
    }

    @Override
    public String generateQuiz(String postContent, String language) {
        return complete("""
                请把这篇学习内容整理成复习卡片。输出 4 张卡片，每张包含：
                - 问题
                - 简短答案
                - 适合回顾的关键词
                语言：%s
                内容：
                %s
                """.formatted(language, postContent), () -> fallback.generateQuiz(postContent, language));
    }

    @Override
    public String formatMarkdown(String content, String language) {
        return complete("""
                你是 StudyForge AI 的文章排版助手。请把用户的纯文字整理成适合学习社区发布的 Markdown。

                严格要求：
                - 只输出 Markdown 正文，不要输出解释、前言或代码围栏。
                - 保留用户原意，不新增事实、不编造数据。
                - 使用用户原文语言；如果原文混合中英，可以保留混合表达。
                - 根据内容自然加入二级/三级标题、列表、引用、表格或代码块。
                - 如果原文已经有链接、代码、步骤、清单，请保留并排得更清楚。
                - 不要插入不存在的图片。
                - 输出要适合直接写入 Markdown 编辑器。

                用户选择的内容语言：%s

                原文：
                %s
                """.formatted(language, content), () -> fallback.formatMarkdown(content, language));
    }

    private String complete(String prompt, Fallback fallbackValue) {
        String apiKey = integrationSettingService.getValue("ai.api_key", "");
        if (apiKey.isBlank()) {
            return fallbackValue.get();
        }

        try {
            String baseUrl = trimSlash(integrationSettingService.getValue("ai.base_url", DEFAULT_BASE_URL));
            String model = integrationSettingService.getValue("ai.chat_model", DEFAULT_MODEL);
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.3,
                    "stream", false
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(AI_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallbackValue.get();
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() || content.asText().isBlank() ? fallbackValue.get() : content.asText();
        } catch (Exception exception) {
            return fallbackValue.get();
        }
    }

    private String trimSlash(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private java.util.Optional<ProxySelector> proxySelector() {
        String proxy = firstNotBlank(
                System.getProperty("https.proxy"),
                System.getProperty("https_proxy"),
                System.getenv("https_proxy"),
                System.getenv("HTTPS_PROXY"),
                System.getenv("http_proxy"),
                System.getenv("HTTP_PROXY")
        );
        if (proxy == null) {
            return java.util.Optional.empty();
        }

        try {
            URI uri = proxy.contains("://") ? URI.create(proxy) : URI.create("http://" + proxy);
            if (uri.getHost() == null) {
                return java.util.Optional.empty();
            }
            int port = uri.getPort();
            if (port <= 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            return java.util.Optional.of(ProxySelector.of(new InetSocketAddress(uri.getHost(), port)));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface Fallback {
        String get();
    }
}
