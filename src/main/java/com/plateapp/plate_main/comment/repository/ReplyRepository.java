package com.plateapp.plate_main.comment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.comment.entity.Fp450Reply;

public interface ReplyRepository extends JpaRepository<Fp450Reply, Integer> {

  // ✅ (기존) 여러 댓글의 replies 한번에 가져오기 - B안에서는 댓글목록에서 안 쓰지만, 남겨둬도 됨
  List<Fp450Reply> findByCommentIdInAndUseYnAndDeletedAtIsNullOrderByReplyIdAsc(
      List<Integer> commentIds, String useYn
  );

  // ✅ B안 핵심: 특정 commentId의 replies 페이징 조회
  Page<Fp450Reply> findByCommentIdAndUseYnAndDeletedAtIsNull(
      Integer commentId, String useYn, Pageable pageable
  );

  // ✅ B안 핵심: 댓글 목록에서 replyCount만 뽑기 (group by)
  interface ReplyCountRow {
    Integer getCommentId();
    Long getCnt();
  }

  @Query("""
      select r.commentId as commentId, count(r) as cnt
        from Fp450Reply r
       where r.commentId in :commentIds
         and r.useYn = 'Y'
         and r.deletedAt is null
       group by r.commentId
  """)
  List<ReplyCountRow> countByCommentIds(@Param("commentIds") List<Integer> commentIds);

  Page<Fp450Reply> findByCommentIdAndUseYn(Integer commentId, String useYn, Pageable pageable);

  long countByCommentIdAndUseYn(Integer commentId, String useYn);

  @Modifying
  @Query("DELETE FROM Fp450Reply r WHERE r.commentId = :commentId")
  void deleteByCommentId(@Param("commentId") Integer commentId);

  @Modifying
  @Query("DELETE FROM Fp450Reply r WHERE r.commentId in :commentIds")
  int deleteByCommentIds(@Param("commentIds") List<Integer> commentIds);

  @Query("select r.username from Fp450Reply r where r.replyId = :replyId")
  Optional<String> findOwnerUsername(@Param("replyId") Integer replyId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Fp450Reply r
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
      update Fp450Reply r
         set r.useYn = 'N',
             r.deletedAt = :now,
             r.updatedAt = :now
       where r.replyId = :replyId
         and r.useYn = 'Y'
         and r.deletedAt is null
  """)
  int softDelete(@Param("replyId") Integer replyId,
                 @Param("now") LocalDateTime now);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Fp450Reply r
         set r.useYn = 'N',
             r.deletedAt = :now,
             r.updatedAt = :now
       where r.commentId = :commentId
         and r.useYn = 'Y'
         and r.deletedAt is null
  """)
  int softDeleteByCommentId(@Param("commentId") Integer commentId,
                            @Param("now") LocalDateTime now);

  // feedId 기준 대댓글 수: fp_470 -> fp_460 join 필요해서 native 추천
  @Query(value = """
    select count(*)
    from fp_470 r
    join fp_460 c on c.comment_id = r.comment_id
    where c.feed_id = :feedId
      and c.use_yn = 'Y' and c.deleted_at is null
      and r.use_yn = 'Y' and r.deleted_at is null
  """, nativeQuery = true)
  long countActiveByFeedId(@Param("feedId") Integer feedId);

  interface FeedReplyCountRow {
    Integer getFeedId();
    Long getCnt();
  }

  @Query(value = """
    select c.feed_id as feedId, count(*) as cnt
    from fp_470 r
    join fp_460 c on c.comment_id = r.comment_id
    where c.feed_id in (:feedIds)
      and c.use_yn = 'Y' and c.deleted_at is null
      and r.use_yn = 'Y' and r.deleted_at is null
    group by c.feed_id
  """, nativeQuery = true)
  List<FeedReplyCountRow> countActiveByFeedIds(@Param("feedIds") List<Integer> feedIds);
}
