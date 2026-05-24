package com.studyforge.interaction.mapper;

import com.studyforge.interaction.entity.Comment;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CommentMapper {
    List<Comment> selectVisibleByPostId(@Param("postId") Long postId);

    int insert(Comment comment);
}
