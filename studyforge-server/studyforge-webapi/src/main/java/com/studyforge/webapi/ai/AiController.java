package com.studyforge.webapi.ai;

import com.studyforge.ai.entity.AiLog;
import com.studyforge.ai.mapper.AiLogMapper;
import com.studyforge.ai.service.AiService;
import com.studyforge.ai.service.AiService.GeneratedCover;
import com.studyforge.ai.service.AiTokenBillingService;
import com.studyforge.ai.service.AiTokenUsageLogger;
import com.studyforge.ai.vo.AiLogVO;
import com.studyforge.ai.vo.AiResultVO;
import com.studyforge.common.api.ApiResponse;
import com.studyforge.common.constants.HttpHeaders;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.content.service.PostQueryService;
import com.studyforge.content.vo.PostDetailVO;
import com.studyforge.interaction.learning.service.FavoriteImportanceService;
import com.studyforge.interaction.learning.service.UserLearningProfileService;
import com.studyforge.system.service.AuthService;
import com.studyforge.system.service.UploadedFileService;
import com.studyforge.webapi.support.UploadStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiService aiService;
    private final AiLogMapper aiLogMapper;
    private final PostQueryService postQueryService;
    private final AuthService authService;
    private final UploadedFileService uploadedFileService;
    private final UserLearningProfileService userLearningProfileService;
    private final FavoriteImportanceService favoriteImportanceService;
    private final AiTokenBillingService tokenBillingService;
    private final AiTokenUsageLogger tokenUsageLogger;
    private final Path imageRoot;

    public AiController(AiService aiService,
                        AiLogMapper aiLogMapper,
                        PostQueryService postQueryService,
                        AuthService authService,
                        UploadedFileService uploadedFileService,
                        UserLearningProfileService userLearningProfileService,
                        FavoriteImportanceService favoriteImportanceService,
                        AiTokenBillingService tokenBillingService,
                        AiTokenUsageLogger tokenUsageLogger) {
        this.aiService = aiService;
        this.aiLogMapper = aiLogMapper;
        this.postQueryService = postQueryService;
        this.authService = authService;
        this.uploadedFileService = uploadedFileService;
        this.userLearningProfileService = userLearningProfileService;
        this.favoriteImportanceService = favoriteImportanceService;
        this.tokenBillingService = tokenBillingService;
        this.tokenUsageLogger = tokenUsageLogger;
        this.imageRoot = UploadStorage.imageRoot();
    }

    @PostMapping("/posts/{postId}/summary")
    public ApiResponse<AiResultVO> summary(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                           @PathVariable("postId") Long postId,
                                           @RequestBody AiLanguageRequest request) {
        Long userId = authService.requireUserId(authorization);
        String contentLanguage = contentLanguage(request);
        String promptLanguage = promptLanguage(request);
        PostDetailVO post = postQueryService.getDetail(postId, contentLanguage);
        String userContext = userLearningProfileService.getMemoryContext(userId);

        String prompt = buildSummaryPrompt(post.content(), promptLanguage, userContext);
        String fullResponse = tokenBillingService.callApiWithUsage(prompt);

        if (fullResponse != null) {
            String text = tokenBillingService.extractTextFromResponse(fullResponse);
            Long logId = logWithId(userId, postId, "SUMMARY", post.content(), text, 1);
            logTokenUsage(logId, fullResponse);
            favoriteImportanceService.recomputeAllForUser(userId, contentLanguage);
            return ApiResponse.success(new AiResultVO("SUMMARY", promptLanguage, text));
        }

        String fallbackText = aiService.generateSummary(post.content(), promptLanguage, userContext);
        log(userId, postId, "SUMMARY", post.content(), fallbackText, 0);
        favoriteImportanceService.recomputeAllForUser(userId, contentLanguage);
        return ApiResponse.success(new AiResultVO("SUMMARY", promptLanguage, fallbackText));
    }

    @PostMapping("/posts/{postId}/review-cards")
    public ApiResponse<AiResultVO> reviewCards(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                               @PathVariable("postId") Long postId,
                                               @RequestBody AiLanguageRequest request) {
        Long userId = authService.requireUserId(authorization);
        String contentLanguage = contentLanguage(request);
        String promptLanguage = promptLanguage(request);
        PostDetailVO post = postQueryService.getDetail(postId, contentLanguage);
        String userContext = userLearningProfileService.getMemoryContext(userId);

        String prompt = buildQuizPrompt(post.content(), promptLanguage, userContext);
        String fullResponse = tokenBillingService.callApiWithUsage(prompt);

        if (fullResponse != null) {
            String text = tokenBillingService.extractTextFromResponse(fullResponse);
            Long logId = logWithId(userId, postId, "REVIEW_CARD", post.content(), text, 1);
            logTokenUsage(logId, fullResponse);
            favoriteImportanceService.recomputeAllForUser(userId, contentLanguage);
            return ApiResponse.success(new AiResultVO("REVIEW_CARD", promptLanguage, text));
        }

        String fallbackText = aiService.generateQuiz(post.content(), promptLanguage, userContext);
        log(userId, postId, "REVIEW_CARD", post.content(), fallbackText, 0);
        favoriteImportanceService.recomputeAllForUser(userId, contentLanguage);
        return ApiResponse.success(new AiResultVO("REVIEW_CARD", promptLanguage, fallbackText));
    }

    @PostMapping("/posts/{postId}/questions")
    public ApiResponse<AiResultVO> answer(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                          @PathVariable("postId") Long postId,
                                          @RequestBody AiQuestionRequest request) {
        Long userId = authService.requireUserId(authorization);
        String contentLanguage = request == null || isBlank(request.contentLanguageCode()) ? answerLanguage(request) : request.contentLanguageCode();
        String promptLanguage = request == null || isBlank(request.promptLanguageCode()) ? answerLanguage(request) : request.promptLanguageCode();
        PostDetailVO post = postQueryService.getDetail(postId, contentLanguage);
        String userContext = userLearningProfileService.getMemoryContext(userId);
        String question = request == null ? "" : request.question();
        String prompt = buildQuestionPrompt(post.content(), question, promptLanguage, userContext);
        String fullResponse = tokenBillingService.callApiWithUsage(prompt);

        if (fullResponse != null) {
            String text = tokenBillingService.extractTextFromResponse(fullResponse);
            Long logId = logWithId(userId, postId, "QUESTION", question, text, 1);
            logTokenUsage(logId, fullResponse);
            favoriteImportanceService.recomputeAllForUser(userId, contentLanguage);
            return ApiResponse.success(new AiResultVO("QUESTION", promptLanguage, text));
        }

        String fallbackText = aiService.answerQuestion(post.content(), question, promptLanguage, userContext);
        log(userId, postId, "QUESTION", question, fallbackText, 0);
        favoriteImportanceService.recomputeAllForUser(userId, contentLanguage);
        return ApiResponse.success(new AiResultVO("QUESTION", promptLanguage, fallbackText));
    }

    @PostMapping("/markdown/format")
    public ApiResponse<AiResultVO> formatMarkdown(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                  @RequestBody AiMarkdownFormatRequest request) {
        Long userId = authService.requireUserId(authorization);
        if (request == null || isBlank(request.content())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "content is required");
        }
        String promptLanguage = isBlank(request.promptLanguageCode())
                ? (isBlank(request.languageCode()) ? "zh_CN" : request.languageCode())
                : request.promptLanguageCode();
        String source = request.content().trim();
        if (source.length() > 12000) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "content is too long");
        }
        
        String prompt = buildMarkdownPrompt(source, promptLanguage);
        String fullResponse = tokenBillingService.callApiWithUsage(prompt);
        
        if (fullResponse != null) {
            String text = tokenBillingService.extractTextFromResponse(fullResponse);
            Long logId = logWithId(userId, null, "MARKDOWN_FORMAT", source, text, 1);
            logTokenUsage(logId, fullResponse);
            return ApiResponse.success(new AiResultVO("MARKDOWN_FORMAT", promptLanguage, text));
        }
        
        // 降级到原生 AiService
        String fallbackText = aiService.formatMarkdown(source, promptLanguage);
        log(userId, null, "MARKDOWN_FORMAT", source, fallbackText, 0);
        return ApiResponse.success(new AiResultVO("MARKDOWN_FORMAT", promptLanguage, fallbackText));
    }

    @PostMapping("/covers/generate")
    public ApiResponse<AiCoverResultVO> generateCover(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                      @RequestBody AiCoverRequest request) throws IOException {
        Long userId = authService.requireUserId(authorization);
        if (request == null || (isBlank(request.title()) && isBlank(request.summary()) && isBlank(request.content()))) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "title, summary or content is required");
        }

        String title = trim(request.title(), 300);
        String summary = trim(request.summary(), 800);
        String content = trim(request.content(), 12000);
        String language = isBlank(request.languageCode()) ? "zh_CN" : request.languageCode();
        GeneratedCover cover = aiService.generateCover(title, summary, content, language);
        if (cover == null || isBlank(cover.imageDataBase64())) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "cover generation failed");
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(cover.imageDataBase64());
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "image data is invalid");
        }
        if (imageBytes.length == 0 || imageBytes.length > 12L * 1024L * 1024L) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "generated image size is invalid");
        }

        Files.createDirectories(imageRoot);
        String filename = UUID.randomUUID() + ".png";
        Path target = imageRoot.resolve(filename).normalize();
        if (!target.startsWith(imageRoot)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "invalid file path");
        }
        Files.write(target, imageBytes);

        String url = "/api/v1/files/images/" + filename;
        uploadedFileService.recordImage(userId, "ai-generated-cover.png", filename, url, "image/png", imageBytes.length);
        log(userId, null, "COVER_IMAGE", cover.prompt(), cover.visualBrief() + "\n" + url, 1);
        return ApiResponse.success(new AiCoverResultVO(url, cover.visualBrief()));
    }

    @GetMapping("/me/review-cards")
    public ApiResponse<List<AiLogVO>> myReviewCards(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                    @RequestParam(name = "limit", defaultValue = "20") int limit) {
        Long userId = authService.requireUserId(authorization);
        int normalizedLimit = limit <= 0 ? 20 : Math.min(limit, 50);
        List<AiLogVO> cards = aiLogMapper.selectByUserAndType(userId, "REVIEW_CARD", normalizedLimit)
                .stream()
                .map(log -> new AiLogVO(log.getLogId(), log.getPostId(), log.getAiType(), log.getResponseText(), log.getSuccess(), log.getCreatedTime()))
                .toList();
        return ApiResponse.success(cards);
    }

    /**
     * 记录 AI 日志并返回 logId
     */
    private Long logWithId(Long userId, Long postId, String aiType, String requestText, String responseText, int success) {
        AiLog log = new AiLog();
        log.setUserId(userId);
        log.setPostId(postId);
        log.setAiType(aiType);
        log.setRequestText(trim(requestText, 4000));
        log.setResponseText(responseText);
        log.setSuccess(success);
        aiLogMapper.insert(log);
        return log.getLogId();
    }

    private void log(Long userId, Long postId, String aiType, String requestText, String responseText, int success) {
        logWithId(userId, postId, aiType, requestText, responseText, success);
    }

    private void logTokenUsage(Long logId, String fullResponse) {
        if (logId != null) {
            tokenUsageLogger.logTokenUsage(logId, fullResponse, tokenBillingService.getCurrentModel());
        }
    }

    private String contentLanguage(AiLanguageRequest request) {
        if (request != null && !isBlank(request.contentLanguageCode())) {
            return request.contentLanguageCode();
        }
        return request == null || isBlank(request.languageCode()) ? "zh_CN" : request.languageCode();
    }

    private String promptLanguage(AiLanguageRequest request) {
        if (request != null && !isBlank(request.promptLanguageCode())) {
            return request.promptLanguageCode();
        }
        return request == null || isBlank(request.languageCode()) ? "zh_CN" : request.languageCode();
    }

    private String answerLanguage(AiQuestionRequest request) {
        return request == null || isBlank(request.answerLanguage()) ? "zh_CN" : request.answerLanguage();
    }

    /**
     * 构建摘要生成的 prompt
     */
    private String buildSummaryPrompt(String content, String language, String userContext) {
        String context = learningContextBlock(userContext, language);
        boolean isEnglish = language != null && language.toLowerCase().startsWith("en");
        if (isEnglish) {
            return """
                    Please summarize this learning post in English. Make the output feel like an AI summary in a real learning product:
                    1. Start with 3 key points.
                    2. Then add one concise takeaway worth saving.
                    3. Do not mention "according to the document" or "as an AI".

                    Website language: English

                    %s
                    Content:
                    %s
                    """.formatted(context, content);
        } else {
            return """
                    请用中文提炼这篇学习内容。输出要像真实学习产品里的 AI 摘要：
                    1. 先给 3 条要点；
                    2. 再给 1 句适合收藏的结论；
                    3. 不要提“根据文档/作为 AI”。

                    网站语言：中文

                    %s
                    内容：
                    %s
                    """.formatted(context, content);
        }
    }

    /**
     * 构建复习卡片生成的 prompt
     */
    private String buildQuizPrompt(String content, String language, String userContext) {
        String context = learningContextBlock(userContext, language);
        boolean isEnglish = language != null && language.toLowerCase().startsWith("en");
        if (isEnglish) {
            return """
                    Turn this learning post into review cards. Output 4 cards. Each card must include:
                    - Question
                    - Short answer
                    - Keywords for review

                    Website language: English

                    %s
                    Content:
                    %s
                    """.formatted(context, content);
        } else {
            return """
                    请把这篇学习内容整理成复习卡片。输出 4 张卡片，每张包含：
                    - 问题
                    - 简短答案
                    - 适合回顾的关键词

                    网站语言：中文

                    %s
                    内容：
                    %s
                    """.formatted(context, content);
        }
    }

    /**
     * 构建问题回答的 prompt
     */
    private String buildQuestionPrompt(String postContent, String question, String language, String userContext) {
        String context = learningContextBlock(userContext, language);
        boolean isEnglish = language != null && language.toLowerCase().startsWith("en");
        if (isEnglish) {
            return """
                    You are StudyForge AI's learning assistant. Answer only from the article content.
                    Answer in English.

                    %s
                    Article:
                    %s

                    Question:
                    %s
                    """.formatted(context, postContent, question);
        } else {
            return """
                    你是 StudyForge AI 的学习助手。请只依据文章内容回答问题。
                    请用中文回答。

                    %s
                    文章：
                    %s

                    问题：
                    %s
                    """.formatted(context, postContent, question);
        }
    }

    private String learningContextBlock(String userContext, String language) {
        if (isBlank(userContext)) {
            return "";
        }
        boolean isEnglish = language != null && language.toLowerCase().startsWith("en");
        if (isEnglish) {
            return """
                    Learner memory context:
                    %s

                    Use this context only when it helps personalize emphasis, examples, or review focus.
                    """.formatted(userContext);
        }
        return """
                学习记忆上下文：
                %s

                仅在有助于个性化重点、示例或复习方向时使用这段上下文。
                """.formatted(userContext);
    }

    /**
     * 构建 Markdown 格式化的 prompt
     */
    private String buildMarkdownPrompt(String content, String language) {
        boolean isEnglish = language != null && language.toLowerCase().startsWith("en");
        if (isEnglish) {
            return """
                    You are StudyForge AI's article formatting assistant. Reformat the user's plain text into Markdown suitable for a learning community post.

                    Strict rules:
                    - Output Markdown body only. Do not add explanations, prefaces, or code fences around the whole answer.
                    - Preserve the user's meaning. Do not add facts or invent data.
                    - Keep the source text's own language. If the source mixes Chinese and English, keep the mixed expression.
                    - Add level-2 or level-3 headings, lists, quotes, tables, or code blocks when they naturally fit.
                    - If the source already includes links, code, steps, or checklists, keep them and make them clearer.
                    - Do not insert images that are not present in the source.
                    - The output should be ready to paste into the Markdown editor.

                    Website language: English

                    Source:
                    %s
                    """.formatted(content);
        } else {
            return """
                    你是 StudyForge AI 的文章排版助手。请把用户的纯文字整理成适合学习社区发布的 Markdown。

                    严格要求：
                    - 只输出 Markdown 正文，不要输出解释、前言或代码围栏。
                    - 保留用户原意，不新增事实、不编造数据。
                    - 使用用户原文语言；如果原文混合中英，可以保留混合表达。
                    - 根据内容自然加入二级/三级标题、列表、引用、表格或代码块。
                    - 如果原文已经有链接、代码、步骤、清单，请保留并排得更清楚。
                    - 不要插入不存在的图片。
                    - 输出要适合直接写入 Markdown 编辑器。

                    网站语言：中文

                    原文：
                    %s
                    """.formatted(content);
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record AiLanguageRequest(String languageCode, String contentLanguageCode, String promptLanguageCode) {
    }

    public record AiQuestionRequest(String question, String answerLanguage, String contentLanguageCode, String promptLanguageCode) {
    }

    public record AiMarkdownFormatRequest(String content,
                                          String languageCode,
                                          String contentLanguageCode,
                                          String promptLanguageCode) {
    }

    public record AiCoverRequest(String title,
                                 String summary,
                                 String content,
                                 String languageCode,
                                 String promptLanguageCode) {
    }

    public record AiCoverResultVO(String coverImageUrl, String visualBrief) {
    }
}
