// src/main/java/com/plateapp/plate_main/comment/repository/Fp440CommentRepository.java
package com.plateapp.plate_main.video.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.comment.entity.Fp440Comment;

public interface Fp440CommentRepository extends JpaRepository<Fp440Comment, Integer> {

    interface StoreCommentCount {
        Integer getStoreId();
        Long getCnt();
    }

    @Query("""
        SELECT c.storeId as storeId, COUNT(c.commentId) as cnt
        FROM Fp440Comment c
        WHERE c.useYn = 'Y'
          AND c.deletedAt IS NULL
          AND c.storeId IN :storeIds
        GROUP BY c.storeId
    """)
    List<StoreCommentCount> countActiveByStoreIds(@Param("storeIds") Collection<Integer> storeIds);
}
