package com.studyforge.ai.service;

public interface AiService {
    default String generateSummary(String content, String language) {
        return generateSummary(content, language, "");
    }

    String generateSummary(String content, String language, String userContext);

    String generateTags(String content, String language);

    String recommendCategory(String content, String language);

    String translateText(String text, String sourceLang, String targetLang);

    String moderateContent(String content, String language);

    default String answerQuestion(String postContent, String question, String answerLanguage) {
        return answerQuestion(postContent, question, answerLanguage, "");
    }

    String answerQuestion(String postContent, String question, String answerLanguage, String userContext);

    default String generateQuiz(String postContent, String language) {
        return generateQuiz(postContent, language, "");
    }

    String generateQuiz(String postContent, String language, String userContext);

    String refreshUserLearningProfile(String signalsPayload, String language);

    String extractPostSemanticTags(String title, String summary, String language);

    String extractMemorySemanticTags(String memoryMd, String language);

    String extractBatchPostSemanticTags(String itemsPayloadJson, String language);

    String formatMarkdown(String content, String language);

    GeneratedCover generateCover(String title, String summary, String content, String language);

    record GeneratedCover(String imageDataBase64, String mimeType, String visualBrief, String prompt) {
    }
}
