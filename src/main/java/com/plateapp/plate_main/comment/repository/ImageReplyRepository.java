package com.plateapp.plate_main.comment.repository;

import com.plateapp.plate_main.comment.entity.ImageReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageReplyRepository extends JpaRepository<ImageReply, Integer> {

    Page<ImageReply> findByCommentIdAndUseYn(Integer commentId, String useYn, Pageable pageable);

    long countByCommentIdAndUseYn(Integer commentId, String useYn);

    @Modifying
    @Query("UPDATE ImageReply r SET r.useYn = 'N', r.deletedAt = CURRENT_TIMESTAMP WHERE r.commentId = :commentId")
    void deleteByCommentId(@Param("commentId") Integer commentId);
}
