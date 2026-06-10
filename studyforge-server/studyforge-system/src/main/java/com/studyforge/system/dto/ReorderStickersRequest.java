package com.studyforge.system.dto;

import java.util.List;

public record ReorderStickersRequest(List<Long> stickerIds) {
}
