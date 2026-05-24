package com.studyforge.ai.service;

public interface AiService {
    String generateSummary(String content, String language);

    String generateTags(String content, String language);

    String recommendCategory(String content, String language);

    String translateText(String text, String sourceLang, String targetLang);

    String moderateContent(String content, String language);

    String answerQuestion(String postContent, String question, String answerLanguage);

    String generateQuiz(String postContent, String language);

    String formatMarkdown(String content, String language);
}
