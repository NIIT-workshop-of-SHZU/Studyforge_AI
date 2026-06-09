package com.studyforge.webapi.collection;

import com.studyforge.common.api.ApiResponse;
import com.studyforge.common.constants.HttpHeaders;
import com.studyforge.content.vo.PostSummaryVO;
import com.studyforge.interaction.dto.CreateFavoriteCollectionRequest;
import com.studyforge.interaction.learning.dto.PatchFavoriteItemRequest;
import com.studyforge.interaction.learning.service.FavoriteImportanceService;
import com.studyforge.interaction.service.FavoriteCollectionService;
import com.studyforge.interaction.vo.FavoriteCollectionVO;
import com.studyforge.system.service.AuthService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collections")
public class FavoriteCollectionController {
    private final AuthService authService;
    private final FavoriteCollectionService favoriteCollectionService;
    private final FavoriteImportanceService favoriteImportanceService;

    public FavoriteCollectionController(AuthService authService,
                                        FavoriteCollectionService favoriteCollectionService,
                                        FavoriteImportanceService favoriteImportanceService) {
        this.authService = authService;
        this.favoriteCollectionService = favoriteCollectionService;
        this.favoriteImportanceService = favoriteImportanceService;
    }

    @GetMapping("/me")
    public ApiResponse<List<FavoriteCollectionVO>> mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(favoriteCollectionService.listMine(userId));
    }

    @PostMapping
    public ApiResponse<FavoriteCollectionVO> create(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                    @RequestBody CreateFavoriteCollectionRequest request) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success("created", favoriteCollectionService.create(userId, request));
    }

    @GetMapping("/{collectionId}/posts")
    public ApiResponse<List<PostSummaryVO>> posts(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                  @PathVariable("collectionId") Long collectionId,
                                                  @RequestParam(name = "languageCode", defaultValue = "zh_CN") String languageCode,
                                                  @RequestParam(name = "sort", defaultValue = "importance") String sort,
                                                  @RequestParam(name = "tag", required = false) String tag,
                                                  @RequestParam(name = "limit", defaultValue = "30") int limit) {
        Long userId = authService.requireUserId(authorization);
        favoriteCollectionService.requireOwner(userId, collectionId);
        return ApiResponse.success(favoriteImportanceService.listCollectionPosts(userId, collectionId, languageCode, limit, sort, tag));
    }

    @PatchMapping("/{collectionId}/posts/{postId}")
    public ApiResponse<Void> patchPost(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                       @PathVariable("collectionId") Long collectionId,
                                       @PathVariable("postId") Long postId,
                                       @RequestParam(name = "languageCode", defaultValue = "zh_CN") String languageCode,
                                       @RequestBody PatchFavoriteItemRequest request) {
        Long userId = authService.requireUserId(authorization);
        favoriteCollectionService.requireOwner(userId, collectionId);
        if (request != null && request.pinned() != null) {
            favoriteImportanceService.setPinned(userId, collectionId, postId, request.pinned(), languageCode);
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/{collectionId}/posts/{postId}")
    public ApiResponse<FavoriteCollectionVO> addPost(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                     @PathVariable("collectionId") Long collectionId,
                                                     @PathVariable("postId") Long postId) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(favoriteCollectionService.addPost(userId, collectionId, postId));
    }

    @DeleteMapping("/{collectionId}/posts/{postId}")
    public ApiResponse<FavoriteCollectionVO> removePost(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                        @PathVariable("collectionId") Long collectionId,
                                                        @PathVariable("postId") Long postId) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(favoriteCollectionService.removePost(userId, collectionId, postId));
    }
}
