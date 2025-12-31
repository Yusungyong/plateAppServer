package com.plateapp.plate_main.comment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.comment.entity.Fp470FeedReply;

public interface FeedReplyRepository extends JpaRepository<Fp470FeedReply, Integer> {

  Page<Fp470FeedReply> findByCommentIdAndUseYnAndDeletedAtIsNull(
      Integer commentId, String useYn, Pageable pageable
  );

  interface ReplyCountRow {
    Integer getCommentId();
    Long getCnt();
  }

  @Query("""
      select r.commentId as commentId, count(r) as cnt
        from Fp470FeedReply r
       where r.commentId in :commentIds
         and r.useYn = 'Y'
         and r.deletedAt is null
       group by r.commentId
  """)
  List<ReplyCountRow> countByCommentIds(@Param("commentIds") List<Integer> commentIds);

  @Query("select r.username from Fp470FeedReply r where r.replyId = :replyId")
  Optional<String> findOwnerUsername(@Param("replyId") Integer replyId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Fp470FeedReply r
         set r.content = :content,
             r.updatedAt = :now
       where r.replyId = :replyId
         and r.useYn = 'Y'
         and r.deletedAt is null
  """)
  int updateContent(@Param("replyId") Integer replyId,
                    @Param("content") String content,
                    @Param("now") LocalDateTime now);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      delete from Fp470FeedReply r
       where r.commentId = :commentId
  """)
  int hardDeleteByCommentId(@Param("commentId") Integer commentId);

  // ✅ feedId 기준 대댓글 수 (fp_470 -> fp_460 join)
  @Query(value = """
    select count(*)
    from fp_470 r
    join fp_460 c on c.comment_id = r.comment_id
    where c.feed_id = :feedId
      and c.use_yn = 'Y' and c.deleted_at is null
      and r.use_yn = 'Y' and r.deleted_at is null
  """, nativeQuery = true)
  long countActiveByFeedId(@Param("feedId") Integer feedId);
}
