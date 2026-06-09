package com.studyforge.system.dto;

public record HomepageDraftRequest(String templateType,
                                   String layoutMode,
                                   String themeConfig,
                                   String customCodeDraft,
                                   String mediaLayoutDraft) {
}
