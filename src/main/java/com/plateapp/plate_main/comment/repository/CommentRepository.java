package com.plateapp.plate_main.comment.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.comment.entity.Fp440Comment;

public interface CommentRepository extends JpaRepository<Fp440Comment, Integer> {

  Page<Fp440Comment> findByStoreIdAndUseYnAndDeletedAtIsNull(
      Integer storeId, String useYn, Pageable pageable
  );

  @Query("select c.username from Fp440Comment c where c.commentId = :commentId")
  Optional<String> findOwnerUsername(@Param("commentId") Integer commentId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Fp440Comment c
         set c.content = :content,
             c.updatedAt = :now
       where c.commentId = :commentId
         and c.useYn = 'Y'
         and c.deletedAt is null
  """)
  int updateContent(@Param("commentId") Integer commentId,
                    @Param("content") String content,
                    @Param("now") LocalDateTime now);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Fp440Comment c
         set c.useYn = 'N',
             c.deletedAt = :today,
             c.updatedAt = :now
       where c.commentId = :commentId
         and c.useYn = 'Y'
         and c.deletedAt is null
  """)
  int softDelete(@Param("commentId") Integer commentId,
                 @Param("today") LocalDate today,
                 @Param("now") LocalDateTime now);

  @Query("""
    select count(c)
    from Fp460Comment c
    where c.feedId = :feedId
      and c.useYn = 'Y'
      and c.deletedAt is null
  """)
  long countActiveByFeedId(@Param("feedId") Integer feedId);
}
