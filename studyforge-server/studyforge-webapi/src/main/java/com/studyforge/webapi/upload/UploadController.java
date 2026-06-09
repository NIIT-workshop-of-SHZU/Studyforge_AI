package com.studyforge.webapi.upload;

import com.studyforge.common.api.ApiResponse;
import com.studyforge.common.constants.HttpHeaders;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.system.service.AuthService;
import com.studyforge.system.service.UploadedFileService;
import com.studyforge.system.entity.UploadedFile;
import com.studyforge.webapi.support.UploadStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class UploadController {
    private static final long MAX_IMAGE_SIZE = 8L * 1024L * 1024L;
    private static final long MAX_HOMEPAGE_MEDIA_SIZE = 50L * 1024L * 1024L;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_HOMEPAGE_MEDIA_EXTENSIONS = Set.of("gif", "mp4", "webm");

    private final AuthService authService;
    private final UploadedFileService uploadedFileService;
    private final Path imageRoot;
    private final Path homepageMediaRoot;

    public UploadController(AuthService authService, UploadedFileService uploadedFileService) {
        this.authService = authService;
        this.uploadedFileService = uploadedFileService;
        this.imageRoot = UploadStorage.imageRoot();
        this.homepageMediaRoot = UploadStorage.homepageMediaRoot();
    }

    @PostMapping(value = "/uploads/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadedFileVO> uploadImage(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                   @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = authService.requireUserId(authorization);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "image file is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "image is too large");
        }

        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "only jpg, png, webp and gif images are supported");
        }

        Files.createDirectories(imageRoot);
        String filename = UUID.randomUUID() + "." + extension;
        Path target = imageRoot.resolve(filename).normalize();
        if (!target.startsWith(imageRoot)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "invalid file path");
        }
        file.transferTo(target);

        String url = "/api/v1/files/images/" + filename;
        UploadedFile uploadedFile = uploadedFileService.recordImage(
                userId,
                file.getOriginalFilename(),
                filename,
                url,
                file.getContentType(),
                file.getSize()
        );

        return ApiResponse.success(toUploadedFileVO(uploadedFile));
    }

    @PostMapping(value = "/uploads/homepage-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadedFileVO> uploadHomepageMedia(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                           @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = authService.requireUserId(authorization);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "homepage media file is required");
        }
        if (file.getSize() > MAX_HOMEPAGE_MEDIA_SIZE) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "homepage media is too large");
        }

        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_HOMEPAGE_MEDIA_EXTENSIONS.contains(extension)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "only gif, mp4 and webm homepage media are supported");
        }
        validateHomepageMediaContentType(extension, file.getContentType());

        Files.createDirectories(homepageMediaRoot);
        String filename = UUID.randomUUID() + "." + extension;
        Path target = homepageMediaRoot.resolve(filename).normalize();
        if (!target.startsWith(homepageMediaRoot)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "invalid file path");
        }
        file.transferTo(target);

        String url = "/api/v1/files/homepage-media/" + filename;
        UploadedFile uploadedFile = uploadedFileService.recordHomepageMedia(
                userId,
                file.getOriginalFilename(),
                filename,
                url,
                file.getContentType(),
                file.getSize()
        );

        return ApiResponse.success(toUploadedFileVO(uploadedFile));
    }

    @GetMapping("/files/images/{filename}")
    public ResponseEntity<ByteArrayResource> image(@PathVariable("filename") String filename) throws IOException {
        return file(filename, imageRoot, "image");
    }

    @GetMapping("/files/homepage-media/{filename}")
    public ResponseEntity<ByteArrayResource> homepageMedia(@PathVariable("filename") String filename) throws IOException {
        return file(filename, homepageMediaRoot, "homepage media");
    }

    private ResponseEntity<ByteArrayResource> file(String filename, Path root, String label) throws IOException {
        if (filename == null || filename.contains("/") || filename.contains("\\")) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "invalid filename");
        }
        UploadedFile uploadedFile = uploadedFileService.getByStoredFilename(filename);
        if (uploadedFile == null) {
            throw new BizException(ErrorCode.NOT_FOUND, label + " not found");
        }
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.exists(file)) {
            throw new BizException(ErrorCode.NOT_FOUND, label + " not found");
        }

        String contentType = uploadedFile.getContentType() == null ? Files.probeContentType(file) : uploadedFile.getContentType();
        MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .body(new ByteArrayResource(Files.readAllBytes(file)));
    }

    private void validateHomepageMediaContentType(String extension, String contentType) {
        if (contentType == null || contentType.isBlank() || "application/octet-stream".equalsIgnoreCase(contentType)) {
            return;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        boolean supported = switch (extension) {
            case "gif" -> normalized.startsWith("image/gif");
            case "mp4" -> normalized.startsWith("video/mp4");
            case "webm" -> normalized.startsWith("video/webm");
            default -> false;
        };
        if (!supported) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "homepage media content type is unsupported");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private UploadedFileVO toUploadedFileVO(UploadedFile uploadedFile) {
        return new UploadedFileVO(
                uploadedFile.getFileId(),
                uploadedFile.getOriginalFilename(),
                uploadedFile.getStoredFilename(),
                uploadedFile.getFileUrl(),
                uploadedFile.getContentType(),
                uploadedFile.getFileSize()
        );
    }

    public record UploadedFileVO(Long fileId,
                                 String originalFilename,
                                 String filename,
                                 String url,
                                 String contentType,
                                 long size) {
    }
}
