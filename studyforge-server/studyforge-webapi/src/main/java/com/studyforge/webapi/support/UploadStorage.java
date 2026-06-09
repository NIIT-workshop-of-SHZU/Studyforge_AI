package com.studyforge.webapi.support;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class UploadStorage {
    private UploadStorage() {
    }

    public static Path imageRoot() {
        return uploadRoot("images");
    }

    public static Path homepageMediaRoot() {
        return uploadRoot("homepage-media");
    }

    private static Path uploadRoot(String child) {
        String configuredUploadDir = firstNotBlank(
                System.getProperty("studyforge.upload.dir"),
                System.getenv("STUDYFORGE_UPLOAD_DIR")
        );
        if (configuredUploadDir != null) {
            return Paths.get(configuredUploadDir).resolve(child).toAbsolutePath().normalize();
        }

        Path workingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path projectRoot = workingDirectory.getParent() == null ? workingDirectory : workingDirectory.getParent();
        return projectRoot.resolve("uploads").resolve(child).normalize();
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
