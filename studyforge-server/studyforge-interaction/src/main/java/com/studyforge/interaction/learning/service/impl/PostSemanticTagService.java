package com.studyforge.interaction.learning.service.impl;

import com.studyforge.ai.service.AiService;
import com.studyforge.content.entity.Post;
import com.studyforge.content.entity.PostI18n;
import com.studyforge.content.mapper.PostI18nMapper;
import com.studyforge.content.mapper.PostMapper;
import com.studyforge.interaction.learning.model.SemanticTag;
import com.studyforge.interaction.learning.support.LearningTextTagger;
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
        PostI18n content = resolveContent(postId, languageCode);
        String resolvedTitle = firstNonBlank(title, content == null ? null : content.getTitle());
        String resolvedSummary = firstNonBlank(summary, content == null ? null : content.getSummary());
        String resolvedLanguage = content == null ? languageCode : content.getLanguageCode();
        String fingerprint = SemanticTagParser.fingerprint(resolvedTitle, resolvedSummary);

        if (content != null
                && content.getSemanticTagsJson() != null
                && !content.getSemanticTagsJson().isBlank()
                && fingerprint.equals(content.getSemanticTagsFingerprint())) {
            List<SemanticTag> cached = SemanticTagParser.decodeCached(content.getSemanticTagsJson());
            if (!cached.isEmpty()) {
                return cached;
            }
        }

        List<SemanticTag> extracted = extractWithAi(resolvedTitle, resolvedSummary, resolvedLanguage);
        if (extracted.isEmpty()) {
            extracted = fallbackTags(resolvedTitle, resolvedSummary, aiTags);
        }

        if (content != null && content.getId() != null && !extracted.isEmpty()) {
            postI18nMapper.updateSemanticTags(
                    postId,
                    content.getLanguageCode(),
                    SemanticTagParser.encodeTags(extracted),
                    fingerprint
            );
        }
        return extracted;
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
