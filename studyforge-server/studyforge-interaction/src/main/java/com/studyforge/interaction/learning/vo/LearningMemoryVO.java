package com.studyforge.interaction.learning.vo;

import com.studyforge.interaction.learning.model.InterestTag;
import java.time.LocalDateTime;
import java.util.List;

public record LearningMemoryVO(String memoryMd,
                               List<InterestTag> interestTags,
                               boolean aiMemoryEnabled,
                               boolean memoryManuallyEdited,
                               LocalDateTime lastRefreshedAt,
                               int profileVersion) {
}
