package com.studyforge.interaction.learning.dto;

import com.studyforge.interaction.learning.model.InterestTag;
import java.util.List;

public record UpdateLearningMemoryRequest(String memoryMd,
                                          List<InterestTag> interestTags,
                                          Boolean aiMemoryEnabled) {
}
