package com.studyforge.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.system.service.IntegrationSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI Token 计费扩展服务
 * 
 * 设计原则：
 * - 独立的 Service，不修改 SiliconFlowAiServiceImpl
 * - 提供带 token 统计的 AI 调用方法
 * - 复用现有的配置和 HTTP 客户端
 */
@Service
public class AiTokenBillingService {
    
    private static final String DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1";
    private static final String DEFAULT_MODEL = "deepseek-ai/DeepSeek-V4-Flash";
    private static final Duration AI_TIMEOUT = Duration.ofSeconds(200);
    
    private final IntegrationSettingService integrationSettingService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    private static final Logger log = LoggerFactory.getLogger(AiTokenBillingService.class);
    
    @Autowired
    public AiTokenBillingService(IntegrationSettingService integrationSettingService) {
        this.integrationSettingService = integrationSettingService;
        this.objectMapper = new ObjectMapper();  // 自己创建，不依赖 Spring 注入
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(AI_TIMEOUT)
                .build();
    }
    
    /**
     * 调用 AI API 并返回完整响应（包含 usage 信息）
     * 
     * @param prompt 提示词
     * @return API 响应的 JSON 字符串
     */
    public String callApiWithUsage(String prompt) {
        String apiKey = integrationSettingService.getValue("ai.api_key", "");
        if (apiKey.isBlank()) {
            return null;
        }
        
        try {
            String baseUrl = trimSlash(integrationSettingService.getValue("ai.base_url", DEFAULT_BASE_URL), DEFAULT_BASE_URL);
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
                return null;
            }
            
            // 返回完整的 JSON 响应（包含 usage）
            return response.body();
            
        } catch (Exception e) {
            log.error("[AI Token Billing] API call failed", e);
            return null;
        }
    }
    
    /**
     * 从完整响应中提取文本内容
     * 
     * @param fullResponse 完整的 API 响应 JSON
     * @return 提取的文本内容
     */
    public String extractTextFromResponse(String fullResponse) {
        if (fullResponse == null || fullResponse.isEmpty()) {
            return "";
        }
        
        try {
            JsonNode root = objectMapper.readTree(fullResponse);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() || content.asText().isBlank() ? "" : content.asText();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 获取当前使用的模型名称
     */
    public String getCurrentModel() {
        return integrationSettingService.getValue("ai.chat_model", DEFAULT_MODEL);
    }
    
    private String trimSlash(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
