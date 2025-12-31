package com.plateapp.plate_main.comment.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.plateapp.plate_main.comment.dto.FeedCommentDtos;
import com.plateapp.plate_main.comment.entity.Fp460FeedComment;
import com.plateapp.plate_main.comment.entity.Fp470FeedReply;
import com.plateapp.plate_main.comment.repository.FeedCommentRepository;
import com.plateapp.plate_main.comment.repository.FeedReplyRepository;
import com.plateapp.plate_main.user.entity.Fp100User;
import com.plateapp.plate_main.user.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedCommentService {

  private final FeedCommentRepository feedCommentRepository;
  private final FeedReplyRepository feedReplyRepository;
  private final MemberRepository memberRepository;

  private void validateContent(String content) {
    if (!StringUtils.hasText(content)) throw new IllegalArgumentException("content is empty");
    if (content.length() > 2000) throw new IllegalArgumentException("content too long");
  }

  // ✅ B안: 댓글 목록은 replyCount만
  @Transactional(readOnly = true)
  public FeedCommentDtos.PageResponse<FeedCommentDtos.CommentResponse> getFeedComments(int feedId, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 50);

    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "commentId"));

    Page<Fp460FeedComment> commentPage =
        feedCommentRepository.findByFeedIdAndUseYnAndDeletedAtIsNull(feedId, "Y", pageable);

    List<Fp460FeedComment> comments = commentPage.getContent();

    List<Integer> commentIds = comments.stream()
        .map(Fp460FeedComment::getCommentId)
        .filter(Objects::nonNull)
        .toList();

    Map<Integer, Long> replyCountMap = new HashMap<>();
    if (!commentIds.isEmpty()) {
      for (FeedReplyRepository.ReplyCountRow row : feedReplyRepository.countByCommentIds(commentIds)) {
        replyCountMap.put(row.getCommentId(), row.getCnt());
      }
    }

    Set<String> usernames = new HashSet<>();
    for (Fp460FeedComment c : comments) if (c.getUsername() != null) usernames.add(c.getUsername());

    Map<String, Fp100User> userMap = memberRepository.findByUsernameIn(usernames).stream()
        .collect(Collectors.toMap(Fp100User::getUsername, u -> u));

    List<FeedCommentDtos.CommentResponse> items = new ArrayList<>();
    for (Fp460FeedComment c : comments) {
      FeedCommentDtos.CommentResponse cr = new FeedCommentDtos.CommentResponse();
      cr.commentId = c.getCommentId();
      cr.feedId = c.getFeedId();
      cr.content = c.getContent();
      cr.createdAt = c.getCreatedAt();
      cr.updatedAt = c.getUpdatedAt();

      cr.author = toProfile(userMap.get(c.getUsername()), c.getUsername(), null);
      cr.replyCount = replyCountMap.getOrDefault(c.getCommentId(), 0L);

      // ✅ B안 정석: replies는 비움(프론트에서 펼칠 때만 조회)
      cr.replies = List.of();

      items.add(cr);
    }

    return new FeedCommentDtos.PageResponse<>(safePage, safeSize, commentPage.getTotalElements(), items);
  }

  // ✅ B안: replies 별도 조회
  @Transactional(readOnly = true)
  public FeedCommentDtos.PageResponse<FeedCommentDtos.ReplyResponse> getCommentReplies(int commentId, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 50);

    Fp460FeedComment parent = feedCommentRepository.findById(commentId)
        .orElseThrow(() -> new NoSuchElementException("comment not found"));
    if (!"Y".equals(parent.getUseYn()) || parent.getDeletedAt() != null) {
      throw new NoSuchElementException("comment not found");
    }

    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "replyId"));

    Page<Fp470FeedReply> replyPage =
        feedReplyRepository.findByCommentIdAndUseYnAndDeletedAtIsNull(commentId, "Y", pageable);

    List<Fp470FeedReply> replies = replyPage.getContent();

    Set<String> usernames = new HashSet<>();
    for (Fp470FeedReply r : replies) if (r.getUsername() != null) usernames.add(r.getUsername());

    Map<String, Fp100User> userMap = memberRepository.findByUsernameIn(usernames).stream()
        .collect(Collectors.toMap(Fp100User::getUsername, u -> u));

    List<FeedCommentDtos.ReplyResponse> items = new ArrayList<>();
    for (Fp470FeedReply r : replies) {
      FeedCommentDtos.ReplyResponse rr = new FeedCommentDtos.ReplyResponse();
      rr.replyId = r.getReplyId();
      rr.commentId = r.getCommentId();
      rr.content = r.getContent();
      rr.createdAt = r.getCreatedAt();
      rr.updatedAt = r.getUpdatedAt();

      rr.author = toProfile(userMap.get(r.getUsername()), r.getUsername(), null);
      items.add(rr);
    }

    return new FeedCommentDtos.PageResponse<>(safePage, safeSize, replyPage.getTotalElements(), items);
  }

  @Transactional
  public int addComment(int feedId, String username, String content) {
    validateContent(content);

    Fp460FeedComment c = new Fp460FeedComment();
    c.setFeedId(feedId);
    c.setUsername(username);
    c.setContent(content);
    c.setUseYn("Y");
    c.setDeletedAt(null);

    return feedCommentRepository.save(c).getCommentId();
  }

  @Transactional
  public void updateComment(int commentId, String username, String content) {
    validateContent(content);

    String owner = feedCommentRepository.findOwnerUsername(commentId)
        .orElseThrow(() -> new NoSuchElementException("comment not found"));

    if (!owner.equals(username)) throw new SecurityException("not owner");

    int updated = feedCommentRepository.updateContent(commentId, content, LocalDateTime.now());
    if (updated == 0) throw new IllegalStateException("comment not updated");
  }

  // ✅ 요구사항: 물리삭제
  @Transactional
  public void deleteComment(int commentId, String username) {
    String owner = feedCommentRepository.findOwnerUsername(commentId)
        .orElseThrow(() -> new NoSuchElementException("comment not found"));

    if (!owner.equals(username)) throw new SecurityException("not owner");

    feedReplyRepository.hardDeleteByCommentId(commentId);
    feedCommentRepository.deleteById(commentId);
  }

  @Transactional
  public int addReply(int commentId, String username, String content) {
    validateContent(content);

    Fp460FeedComment parent = feedCommentRepository.findById(commentId)
        .orElseThrow(() -> new NoSuchElementException("comment not found"));
    if (!"Y".equals(parent.getUseYn()) || parent.getDeletedAt() != null) {
      throw new NoSuchElementException("comment not found");
    }

    Fp470FeedReply r = new Fp470FeedReply();
    r.setCommentId(commentId);
    r.setUsername(username);
    r.setContent(content);
    r.setUseYn("Y");
    r.setDeletedAt(null);

    return feedReplyRepository.save(r).getReplyId();
  }

  @Transactional
  public void updateReply(int replyId, String username, String content) {
    validateContent(content);

    String owner = feedReplyRepository.findOwnerUsername(replyId)
        .orElseThrow(() -> new NoSuchElementException("reply not found"));

    if (!owner.equals(username)) throw new SecurityException("not owner");

    int updated = feedReplyRepository.updateContent(replyId, content, LocalDateTime.now());
    if (updated == 0) throw new IllegalStateException("reply not updated");
  }

  // ✅ 요구사항: 물리삭제
  @Transactional
  public void deleteReply(int replyId, String username) {
    String owner = feedReplyRepository.findOwnerUsername(replyId)
        .orElseThrow(() -> new NoSuchElementException("reply not found"));

    if (!owner.equals(username)) throw new SecurityException("not owner");

    feedReplyRepository.deleteById(replyId);
  }

  private FeedCommentDtos.UserProfile toProfile(Fp100User u, String fallbackUsername, Integer fallbackUserId) {
    FeedCommentDtos.UserProfile p = new FeedCommentDtos.UserProfile();
    if (u != null) {
      p.username = u.getUsername();
      p.userId = u.getUserId();
      p.nickName = u.getNickName();
      p.profileImageUrl = u.getProfileImageUrl();
      p.isPrivate = u.getIsPrivate();
    } else {
      p.username = fallbackUsername;
      p.userId = fallbackUserId;
      p.nickName = null;
      p.profileImageUrl = null;
      p.isPrivate = null;
    }
    return p;
  }
}
