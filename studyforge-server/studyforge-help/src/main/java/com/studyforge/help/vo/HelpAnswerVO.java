package com.studyforge.help.vo;

import java.time.LocalDateTime;

public record HelpAnswerVO(Long answerId,
                           Long helpId,
                           Long userId,
                           String content,
                           Integer accepted,
                           LocalDateTime createdTime) {
}
