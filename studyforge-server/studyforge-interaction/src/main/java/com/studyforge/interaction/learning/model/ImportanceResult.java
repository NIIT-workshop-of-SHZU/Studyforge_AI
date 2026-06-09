package com.studyforge.interaction.learning.model;

import java.util.List;

public record ImportanceResult(double score, List<String> rankReasons) {
}
