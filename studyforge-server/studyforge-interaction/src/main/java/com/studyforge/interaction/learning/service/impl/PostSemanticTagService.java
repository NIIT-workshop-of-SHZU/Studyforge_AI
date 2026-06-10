package com.studyforge.interaction.learning.service.impl;

import com.studyforge.ai.service.AiService;
import com.studyforge.content.entity.Post;
import com.studyforge.content.entity.PostI18n;
import com.studyforge.content.mapper.PostI18nMapper;
import com.studyforge.content.mapper.PostMapper;
import com.studyforge.interaction.learning.model.SemanticTag;
import com.studyforge.interaction.learning.support.LearningTextTagger;
import com.studyforge.interaction.learning.support.SemanticPostTagEnricher;
import com.studyforge.interaction.learning.support.SemanticTagParser;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PostSemanticTagService {
    private final AiService aiService;
    private final PostI18nMapper postI18nMapper;
    private final PostMapper postMapper;

    public PostSemanticTagService(AiService aiService,
                                  PostI18nMapper postI18nMapper,
                                  PostMapper postMapper) {
        this.aiService = aiService;
        this.postI18nMapper = postI18nMapper;
        this.postMapper = postMapper;
    }

    public List<SemanticTag> resolveTags(Long postId,
                                         String languageCode,
                                         String title,
                                         String summary,
                                         String aiTags) {
        return resolveTags(postId, languageCode, title, summary, aiTags, false);
    }

    public List<SemanticTag> tagsForDisplay(Long postId,
                                            String languageCode,
                                            String title,
                                            String summary,
                                            String aiTags) {
        ResolvedContent resolved = resolveInputs(postId, languageCode, title, summary, aiTags);
        List<SemanticTag> tags = resolveTags(
                postId,
                languageCode,
                resolved.title(),
                resolved.summary(),
                resolved.aiTags(),
                false
        );
        return SemanticPostTagEnricher.enrich(tags, resolved.title(), resolved.summary(), resolved.aiTags());
    }

    public List<SemanticTag> tagsForScoring(Long postId,
                                            String languageCode,
                                            String title,
                                            String summary,
                                            String aiTags) {
        return tagsForDisplay(postId, languageCode, title, summary, aiTags);
    }

    public boolean needsLlmWarmup(Long postId, String languageCode, String title, String summary) {
        ResolvedContent resolved = resolveInputs(postId, languageCode, title, summary, "");
        PostI18n content = resolved.content();
        String fingerprint = SemanticTagParser.fingerprint(resolved.title(), resolved.summary());
        if (content == null
                || content.getSemanticTagsJson() == null
                || content.getSemanticTagsJson().isBlank()) {
            return true;
        }
        String storedFingerprint = content.getSemanticTagsFingerprint();
        if (!matchesFingerprint(fingerprint, storedFingerprint)) {
            return true;
        }
        return isRuleCache(storedFingerprint);
    }

    public List<SemanticTag> resolveTags(Long postId,
                                         String languageCode,
                                         String title,
                                         String summary,
                                         String aiTags,
                                         boolean allowLlm) {
        ResolvedContent resolved = resolveInputs(postId, languageCode, title, summary, aiTags);
        PostI18n content = resolved.content();
        String fingerprint = SemanticTagParser.fingerprint(resolved.title(), resolved.summary());

        if (content != null && isCacheHit(content, fingerprint)) {
            List<SemanticTag> cached = SemanticTagParser.decodeCached(content.getSemanticTagsJson());
            if (!cached.isEmpty()) {
                return cached;
            }
        }

        List<SemanticTag> extracted = allowLlm
                ? extractWithAi(resolved.title(), resolved.summary(), resolved.languageCode())
                : List.of();
        if (extracted.isEmpty()) {
            extracted = fallbackTags(resolved.title(), resolved.summary(), resolved.aiTags());
        }

        if (content != null && content.getId() != null && !extracted.isEmpty() && shouldPersist(content, fingerprint, allowLlm)) {
            String storedFingerprint = allowLlm ? fingerprint : ruleFingerprint(fingerprint);
            postI18nMapper.updateSemanticTags(
                    postId,
                    content.getLanguageCode(),
                    SemanticTagParser.encodeTags(extracted),
                    storedFingerprint
            );
        }
        return extracted;
    }

    private boolean shouldPersist(PostI18n content, String fingerprint, boolean allowLlm) {
        if (!isCacheHit(content, fingerprint)) {
            return true;
        }
        return allowLlm && isRuleCache(content.getSemanticTagsFingerprint());
    }

    private boolean isCacheHit(PostI18n content, String fingerprint) {
        if (content == null
                || content.getSemanticTagsJson() == null
                || content.getSemanticTagsJson().isBlank()) {
            return false;
        }
        return matchesFingerprint(fingerprint, content.getSemanticTagsFingerprint());
    }

    private static boolean matchesFingerprint(String fingerprint, String storedFingerprint) {
        if (storedFingerprint == null || storedFingerprint.isBlank()) {
            return false;
        }
        return fingerprint.equals(storedFingerprint) || ruleFingerprint(fingerprint).equals(storedFingerprint);
    }

    private static boolean isRuleCache(String storedFingerprint) {
        return storedFingerprint != null && storedFingerprint.endsWith(":rule");
    }

    private static String ruleFingerprint(String fingerprint) {
        return fingerprint + ":rule";
    }

    private ResolvedContent resolveInputs(Long postId,
                                          String languageCode,
                                          String title,
                                          String summary,
                                          String aiTags) {
        PostI18n content = resolveContent(postId, languageCode);
        String resolvedTitle = firstNonBlank(title, content == null ? null : content.getTitle());
        String resolvedSummary = firstNonBlank(summary, content == null ? null : content.getSummary());
        String resolvedAiTags = firstNonBlank(aiTags, content == null ? null : content.getAiTags());
        String resolvedLanguage = content == null ? languageCode : content.getLanguageCode();
        return new ResolvedContent(content, resolvedTitle, resolvedSummary, resolvedAiTags, resolvedLanguage);
    }

    private record ResolvedContent(PostI18n content,
                                   String title,
                                   String summary,
                                   String aiTags,
                                   String languageCode) {
    }

    private List<SemanticTag> extractWithAi(String title, String summary, String language) {
        String raw = aiService.extractPostSemanticTags(title, summary, language);
        return SemanticTagParser.parseTags(raw);
    }

    private List<SemanticTag> fallbackTags(String title, String summary, String aiTags) {
        List<SemanticTag> tags = new ArrayList<>();
        for (String tag : LearningTextTagger.extractPostTags(title, summary, aiTags)) {
            tags.add(new SemanticTag(tag, 0.65));
            if (tags.size() >= 8) {
                break;
            }
        }
        return tags;
    }

    private PostI18n resolveContent(Long postId, String languageCode) {
        PostI18n content = postI18nMapper.selectByPostIdAndLanguage(postId, languageCode);
        if (content != null) {
            return content;
        }
        Post post = postMapper.selectById(postId);
        if (post == null || post.getOriginalLanguage() == null) {
            return null;
        }
        return postI18nMapper.selectByPostIdAndLanguage(postId, post.getOriginalLanguage());
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

}
