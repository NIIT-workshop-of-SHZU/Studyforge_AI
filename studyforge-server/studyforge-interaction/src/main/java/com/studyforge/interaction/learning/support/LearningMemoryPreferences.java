package com.studyforge.interaction.learning.support;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parses explicit preferences from free-form MEMORY.md text (Chinese or English).
 */
public record LearningMemoryPreferences(boolean prefersChinese, boolean dislikesEnglish) {
    private static final Pattern DISLIKES_ENGLISH = Pattern.compile(
            "(不(喜欢|爱|想|愿|要)|讨厌|避免|拒绝|不想|不要|尽量少|不太).{0,10}(英文|英语|english)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern PREFERS_CHINESE = Pattern.compile(
            "(喜欢|爱看|偏好|优先|更想).{0,10}(中文|汉语|简体|国语|chinese)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public static LearningMemoryPreferences parse(String memoryMd) {
        if (memoryMd == null || memoryMd.isBlank()) {
            return new LearningMemoryPreferences(false, false);
        }
        String normalized = memoryMd.toLowerCase(Locale.ROOT);
        boolean dislikesEnglish = DISLIKES_ENGLISH.matcher(normalized).find()
                || normalized.contains("不喜欢读英文")
                || normalized.contains("不想读英文");
        boolean prefersChinese = dislikesEnglish
                || PREFERS_CHINESE.matcher(normalized).find()
                || normalized.contains("中文文章")
                || normalized.contains("中文内容");
        return new LearningMemoryPreferences(prefersChinese, dislikesEnglish);
    }

    public boolean hasLanguagePreference() {
        return prefersChinese || dislikesEnglish;
    }
}
