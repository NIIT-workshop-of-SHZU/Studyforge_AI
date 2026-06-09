package com.studyforge.interaction.learning.service;

import com.studyforge.interaction.learning.dto.UpdateLearningMemoryRequest;
import com.studyforge.interaction.learning.model.InterestTag;
import com.studyforge.interaction.learning.vo.LearningMemoryVO;
import java.util.List;

public interface UserLearningProfileService {
    LearningMemoryVO getMine(Long userId);

    LearningMemoryVO updateMine(Long userId, UpdateLearningMemoryRequest request);

    LearningMemoryVO refreshMine(Long userId, String languageCode);

    void assertRefreshQuota(Long userId);

    LearningMemoryVO applyGeneratedProfile(Long userId, String memoryMd, List<InterestTag> autoTags);

    void ensureProfile(Long userId, String languageCode);

    void syncAutoSignals(Long userId, String languageCode);

    String getMemoryContext(Long userId);

    String buildRefreshSignals(Long userId, String languageCode);
}
