package com.studyforge.interaction.service.impl;

import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.content.entity.Post;
import com.studyforge.content.mapper.PostMapper;
import com.studyforge.interaction.dto.CreateCommentRequest;
import com.studyforge.interaction.entity.Comment;
import com.studyforge.interaction.mapper.CommentMapper;
import com.studyforge.interaction.mapper.PostFavoriteMapper;
import com.studyforge.interaction.mapper.PostLikeMapper;
import com.studyforge.interaction.mapper.PostViewHistoryMapper;
import com.studyforge.interaction.service.InteractionCommandService;
import com.studyforge.interaction.vo.CommentVO;
import com.studyforge.interaction.vo.PostInteractionStateVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InteractionCommandServiceImpl implements InteractionCommandService {
    private static final String DEFAULT_LANGUAGE = "zh_CN";

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostFavoriteMapper postFavoriteMapper;
    private final PostViewHistoryMapper postViewHistoryMapper;

    public InteractionCommandServiceImpl(PostMapper postMapper,
                                         CommentMapper commentMapper,
                                         PostLikeMapper postLikeMapper,
                                         PostFavoriteMapper postFavoriteMapper,
                                         PostViewHistoryMapper postViewHistoryMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.postLikeMapper = postLikeMapper;
        this.postFavoriteMapper = postFavoriteMapper;
        this.postViewHistoryMapper = postViewHistoryMapper;
    }

    @Override
    @Transactional
    public PostInteractionStateVO like(Long postId, Long userId) {
        assertPost(postId);
        if (postLikeMapper.countByPostAndUser(postId, userId) > 0) {
            postLikeMapper.deleteByPostAndUser(postId, userId);
            postMapper.incrementLikeCount(postId, -1);
        } else if (postLikeMapper.insertIgnore(postId, userId) > 0) {
            postMapper.incrementLikeCount(postId, 1);
        }
        return state(postId, userId);
    }

    @Override
    @Transactional
    public PostInteractionStateVO favorite(Long postId, Long userId) {
        assertPost(postId);
        if (postFavoriteMapper.countByPostAndUser(postId, userId) > 0) {
            postFavoriteMapper.deleteByPostAndUser(postId, userId);
            postMapper.incrementFavoriteCount(postId, -1);
        } else if (postFavoriteMapper.insertIgnore(postId, userId) > 0) {
            postMapper.incrementFavoriteCount(postId, 1);
        }
        return state(postId, userId);
    }

    @Override
    public PostInteractionStateVO state(Long postId, Long userId) {
        Post post = assertPost(postId);
        boolean liked = userId != null && postLikeMapper.countByPostAndUser(postId, userId) > 0;
        boolean favorited = userId != null && postFavoriteMapper.countByPostAndUser(postId, userId) > 0;
        return new PostInteractionStateVO(
                liked,
                favorited,
                safeInt(post.getLikeCount()),
                safeInt(post.getFavoriteCount()),
                safeInt(post.getCommentCount()),
                safeInt(post.getViewCount())
        );
    }

    @Override
    @Transactional
    public void recordView(Long postId, Long userId) {
        assertPost(postId);
        postViewHistoryMapper.insert(postId, userId);
        postMapper.incrementViewCount(postId);
    }

    @Override
    @Transactional
    public CommentVO comment(Long postId, Long userId, CreateCommentRequest request) {
        assertPost(postId);
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "comment content is required");
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setLanguageCode(isBlank(request.languageCode()) ? DEFAULT_LANGUAGE : request.languageCode().trim());
        comment.setContent(request.content().trim());
        comment.setStatus("VISIBLE");
        commentMapper.insert(comment);
        postMapper.incrementCommentCount(postId, 1);
        return toVO(comment);
    }

    @Override
    public List<CommentVO> comments(Long postId) {
        assertPost(postId);
        return commentMapper.selectVisibleByPostId(postId)
                .stream()
                .map(this::toVO)
                .toList();
    }

    private Post assertPost(Long postId) {
        if (postId == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "postId is required");
        }
        Post post = postMapper.selectById(postId);
        if (post == null || !"PUBLISHED".equals(post.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "post not found");
        }
        return post;
    }

    private CommentVO toVO(Comment comment) {
        return new CommentVO(
                comment.getCommentId(),
                comment.getPostId(),
                comment.getUserId(),
                comment.getLanguageCode(),
                comment.getContent(),
                comment.getCreatedTime()
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
