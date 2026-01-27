package com.plateapp.plate_main.comment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.comment.entity.Fp460FeedComment;

public interface FeedCommentRepository extends JpaRepository<Fp460FeedComment, Integer> {

  Page<Fp460FeedComment> findByFeedIdAndUseYnAndDeletedAtIsNull(
      Integer feedId, String useYn, Pageable pageable
  );

  @Query("select c.username from Fp460FeedComment c where c.commentId = :commentId")
  Optional<String> findOwnerUsername(@Param("commentId") Integer commentId);

  @Query("select c.commentId from Fp460FeedComment c where c.feedId = :feedId")
  List<Integer> findIdsByFeedId(@Param("feedId") Integer feedId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Fp460FeedComment c
         set c.content = :content,
             c.updatedAt = :now
       where c.commentId = :commentId
         and c.useYn = 'Y'
         and c.deletedAt is null
  """)
  int updateContent(@Param("commentId") Integer commentId,
                    @Param("content") String content,
                    @Param("now") LocalDateTime now);

  @Modifying
  @Query("delete from Fp460FeedComment c where c.feedId = :feedId")
  int hardDeleteByFeedId(@Param("feedId") Integer feedId);

  @Query("""
    select count(c)
    from Fp460FeedComment c
    where c.feedId = :feedId
      and c.useYn = 'Y'
      and c.deletedAt is null
  """)
  long countActiveByFeedId(@Param("feedId") Integer feedId);
}
