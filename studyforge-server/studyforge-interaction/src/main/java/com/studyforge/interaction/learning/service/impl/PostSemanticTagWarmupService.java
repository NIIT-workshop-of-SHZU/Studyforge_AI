package com.studyforge.interaction.learning.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.ai.service.AiService;
import com.studyforge.content.entity.PostI18n;
import com.studyforge.content.mapper.PostI18nMapper;
import com.studyforge.interaction.learning.entity.FavoriteItemRankRow;
import com.studyforge.interaction.learning.mapper.LearningSignalMapper;
import com.studyforge.interaction.learning.model.SemanticTag;
import com.studyforge.interaction.learning.support.SemanticTagParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PostSemanticTagWarmupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostSemanticTagWarmupService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "post-semantic-tag-warmup");
        thread.setDaemon(true);
        return thread;
    });
    private final AiService aiService;
    private final LearningSignalMapper learningSignalMapper;
    private final PostI18nMapper postI18nMapper;
    private final PostSemanticTagService postSemanticTagService;

    public PostSemanticTagWarmupService(AiService aiService,
                                        LearningSignalMapper learningSignalMapper,
                                        PostI18nMapper postI18nMapper,
                                        PostSemanticTagService postSemanticTagService) {
        this.aiService = aiService;
        this.learningSignalMapper = learningSignalMapper;
        this.postI18nMapper = postI18nMapper;
        this.postSemanticTagService = postSemanticTagService;
    }

    public void warmCollectionAsync(Long userId, Long collectionId, String languageCode) {
        executor.submit(() -> warmCollection(userId, collectionId, languageCode));
    }

    public void warmPostAsync(Long postId, String languageCode, String title, String summary) {
        executor.submit(() -> postSemanticTagService.resolveTags(postId, languageCode, title, summary, "", true));
    }

    private void warmCollection(Long userId, Long collectionId, String languageCode) {
        try {
            List<FavoriteItemRankRow> rows = learningSignalMapper.selectCollectionItems(
                    userId,
                    collectionId,
                    languageCode,
                    null,
                    "importance",
                    50
            );
            List<BatchItem> pending = new ArrayList<>();
            for (FavoriteItemRankRow row : rows) {
                if (!postSemanticTagService.needsLlmWarmup(
                        row.getPostId(),
                        languageCode,
                        row.getTitle(),
                        row.getSummary())) {
                    continue;
                }
                pending.add(new BatchItem(
                        row.getPostId(),
                        blank(row.getTitle()),
                        blank(row.getSummary())
                ));
            }
            if (pending.isEmpty()) {
                return;
            }
            if (pending.size() == 1) {
                BatchItem item = pending.get(0);
                postSemanticTagService.resolveTags(item.postId(), languageCode, item.title(), item.summary(), "", true);
                return;
            }
            warmBatch(pending, languageCode);
        } catch (Exception exception) {
            LOGGER.warn("post semantic tag warmup failed for collection {}: {}", collectionId, exception.getMessage());
        }
    }

    private void warmBatch(List<BatchItem> pending, String languageCode) {
        try {
            String payload = OBJECT_MAPPER.writeValueAsString(pending);
            String raw = aiService.extractBatchPostSemanticTags(payload, languageCode);
            List<BatchResult> results = parseBatchResults(raw);
            if (results.isEmpty()) {
                for (BatchItem item : pending) {
                    postSemanticTagService.resolveTags(item.postId(), languageCode, item.title(), item.summary(), "", true);
                }
                return;
            }
            Map<Long, BatchItem> itemMap = new LinkedHashMap<>();
            for (BatchItem item : pending) {
                itemMap.put(item.postId(), item);
            }
            for (BatchResult result : results) {
                BatchItem item = itemMap.get(result.postId());
                if (item == null || result.tags() == null || result.tags().isEmpty()) {
                    continue;
                }
                persistTags(item.postId(), languageCode, item.title(), item.summary(), result.tags());
            }
        } catch (Exception exception) {
            LOGGER.warn("batch semantic tag warmup failed, falling back to single calls: {}", exception.getMessage());
            for (BatchItem item : pending) {
                postSemanticTagService.resolveTags(item.postId(), languageCode, item.title(), item.summary(), "", true);
            }
        }
    }

    private void persistTags(Long postId, String languageCode, String title, String summary, List<SemanticTag> tags) {
        PostI18n content = postI18nMapper.selectByPostIdAndLanguage(postId, languageCode);
        if (content == null || content.getId() == null) {
            return;
        }
        postI18nMapper.updateSemanticTags(
                postId,
                content.getLanguageCode(),
                SemanticTagParser.encodeTags(tags),
                SemanticTagParser.fingerprint(title, summary)
        );
    }

    private List<BatchResult> parseBatchResults(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(extractJsonArray(raw));
            if (!root.isArray()) {
                return List.of();
            }
            List<BatchResult> results = new ArrayList<>();
            for (JsonNode node : root) {
                long postId = node.path("postId").asLong(-1);
                if (postId <= 0) {
                    continue;
                }
                JsonNode tagsNode = node.path("tags");
                if (!tagsNode.isArray()) {
                    continue;
                }
                List<SemanticTag> tags = OBJECT_MAPPER.convertValue(tagsNode, new TypeReference<List<SemanticTag>>() {
                });
                results.add(new BatchResult(postId, SemanticTagParser.decodeCached(SemanticTagParser.encodeTags(tags))));
            }
            return results;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String extractJsonArray(String raw) {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private record BatchItem(Long postId, String title, String summary) {
    }

    private record BatchResult(Long postId, List<SemanticTag> tags) {
    }
}
