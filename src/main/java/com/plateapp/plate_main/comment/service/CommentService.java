package com.plateapp.plate_main.comment.service;

import com.plateapp.plate_main.comment.dto.CommentDtos;
import com.plateapp.plate_main.comment.entity.Fp440Comment;
import com.plateapp.plate_main.comment.entity.Fp450Reply;
import com.plateapp.plate_main.comment.repository.CommentRepository;
import com.plateapp.plate_main.comment.repository.ReplyRepository;
import com.plateapp.plate_main.notification.service.NotificationCommandService;
import com.plateapp.plate_main.user.entity.Fp100User;
import com.plateapp.plate_main.user.repository.MemberRepository;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final ReplyRepository replyRepository;
  private final MemberRepository memberRepository;
  private final Fp300StoreRepository storeRepository;
  private final NotificationCommandService notificationCommandService;

  private void validateContent(String content) {
    if (!StringUtils.hasText(content)) {
      throw new IllegalArgumentException("content is empty");
    }
    if (content.length() > 2000) {
      throw new IllegalArgumentException("content too long");
    }
  }

  @Transactional(readOnly = true)
  public CommentDtos.PageResponse<CommentDtos.CommentResponse> getStoreComments(int storeId, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 50);

    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "commentId"));

    Page<Fp440Comment> commentPage =
        commentRepository.findByStoreIdAndUseYnAndDeletedAtIsNull(storeId, "Y", pageable);

    List<Fp440Comment> comments = commentPage.getContent();

    List<Integer> commentIds = comments.stream()
        .map(Fp440Comment::getCommentId)
        .filter(Objects::nonNull)
        .toList();

    Map<Integer, Long> replyCountMap = new HashMap<>();
    if (!commentIds.isEmpty()) {
      for (ReplyRepository.ReplyCountRow row : replyRepository.countByCommentIds(commentIds)) {
        replyCountMap.put(row.getCommentId(), row.getCnt());
      }
    }

    Set<String> usernames = new HashSet<>();
    for (Fp440Comment c : comments) {
      if (c.getUsername() != null) {
        usernames.add(c.getUsername());
      }
    }

    Map<String, Fp100User> userMap = memberRepository.findByUsernameIn(usernames).stream()
        .collect(Collectors.toMap(Fp100User::getUsername, u -> u));

    List<CommentDtos.CommentResponse> items = new ArrayList<>();

    for (Fp440Comment c : comments) {
      CommentDtos.CommentResponse cr = new CommentDtos.CommentResponse();
      cr.commentId = c.getCommentId();
      cr.storeId = c.getStoreId();
      cr.content = c.getContent();
      cr.createdAt = c.getCreatedAt();
      cr.updatedAt = c.getUpdatedAt();
      cr.author = toProfile(userMap.get(c.getUsername()), c.getUsername(), c.getUserId());
      cr.replyCount = replyCountMap.getOrDefault(c.getCommentId(), 0L);
      items.add(cr);
    }

    return new CommentDtos.PageResponse<>(safePage, safeSize, commentPage.getTotalElements(), items);
  }

  @Transactional(readOnly = true)
  public CommentDtos.PageResponse<CommentDtos.ReplyResponse> getCommentReplies(int commentId, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 50);

    Fp440Comment parent = commentRepository.findById(commentId)
        .orElseThrow(() -> new NoSuchElementException("comment not found"));

    if (!"Y".equals(parent.getUseYn()) || parent.getDeletedAt() != null) {
      throw new NoSuchElementException("comment not found");
    }

    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "replyId"));

    Page<Fp450Reply> replyPage =
        replyRepository.findByCommentIdAndUseYnAndDeletedAtIsNull(commentId, "Y", pageable);

    List<Fp450Reply> replies = replyPage.getContent();

    Set<String> usernames = new HashSet<>();
    for (Fp450Reply r : replies) {
      if (r.getUsername() != null) {
        usernames.add(r.getUsername());
      }
    }

    Map<String, Fp100User> userMap = memberRepository.findByUsernameIn(usernames).stream()
        .collect(Collectors.toMap(Fp100User::getUsername, u -> u));

    List<CommentDtos.ReplyResponse> items = new ArrayList<>();
    for (Fp450Reply r : replies) {
      CommentDtos.ReplyResponse rr = new CommentDtos.ReplyResponse();
      rr.replyId = r.getReplyId();
      rr.commentId = r.getCommentId();
      rr.content = r.getContent();
      rr.createdAt = r.getCreatedAt();
      rr.updatedAt = r.getUpdatedAt();
      rr.author = toProfile(userMap.get(r.getUsername()), r.getUsername(), null);
      items.add(rr);
    }

    return new CommentDtos.PageResponse<>(safePage, safeSize, replyPage.getTotalElements(), items);
  }

  @Transactional
  public int addComment(int storeId, String username, Integer userId, String content) {
    validateContent(content);

    Fp440Comment c = new Fp440Comment();
    c.setStoreId(storeId);
    c.setUsername(username);
    c.setUserId(userId);
    c.setContent(content);
    c.setUseYn("Y");
    c.setDeletedAt(null);

    Fp440Comment saved = commentRepository.save(c);

    storeRepository.findById(storeId)
        .map(Fp300Store::getUsername)
        .filter(ownerUsername -> ownerUsername != null && !ownerUsername.equals(username))
        .ifPresent(ownerUsername -> notificationCommandService.notifyStoreComment(username, ownerUsername, storeId, saved.getCommentId()));

    return saved.getCommentId();
  }

  @Transactional
  public void updateComment(int commentId, String username, String content) {
    validateContent(content);

    String owner = commentRepository.findOwnerUsername(commentId)
        .orElseThrow(() -> new NoSuchElementException("comment not found"));

    if (!owner.equals(username)) {
      throw new SecurityException("not owner");
    }

    int updated = commentRepository.updateContent(commentId, content, LocalDateTime.now());
    if (updated == 0) {
      throw new IllegalStateException("comment not updated");
    }
  }

  @Transactional
  public void deleteComment(int commentId, String username) {
    String owner = commentRepository.findOwnerUsername(commentId)
        .orElseThrow(() -> new NoSuchElementException("comment not found"));

    if (!owner.equals(username)) {
      throw new SecurityException("not owner");
    }

    replyRepository.softDeleteByCommentId(commentId, LocalDateTime.now());

    int deleted = commentRepository.softDelete(commentId, LocalDate.now(), LocalDateTime.now());
    if (deleted == 0) {
      throw new IllegalStateException("comment not deleted");
    }
  }

  @Transactional
  public int addReply(int commentId, String username, String content) {
    validateContent(content);

    Fp440Comment parent = commentRepository.findById(commentId)
        .orElseThrow(() -> new NoSuchElementException("comment not found"));

    if (!"Y".equals(parent.getUseYn()) || parent.getDeletedAt() != null) {
      throw new NoSuchElementException("comment not found");
    }

    Fp450Reply r = new Fp450Reply();
    r.setCommentId(commentId);
    r.setUsername(username);
    r.setContent(content);
    r.setUseYn("Y");
    r.setDeletedAt(null);

    Fp450Reply saved = replyRepository.save(r);

    String receiverUsername = parent.getUsername();
    if (receiverUsername != null && !receiverUsername.equals(username)) {
      notificationCommandService.notifyStoreReply(username, receiverUsername, parent.getStoreId(), parent.getCommentId(), saved.getReplyId());
    }

    return saved.getReplyId();
  }

  @Transactional
  public void updateReply(int replyId, String username, String content) {
    validateContent(content);

    String owner = replyRepository.findOwnerUsername(replyId)
        .orElseThrow(() -> new NoSuchElementException("reply not found"));

    if (!owner.equals(username)) {
      throw new SecurityException("not owner");
    }

    int updated = replyRepository.updateContent(replyId, content, LocalDateTime.now());
    if (updated == 0) {
      throw new IllegalStateException("reply not updated");
    }
  }

  @Transactional
  public void deleteReply(int replyId, String username) {
    String owner = replyRepository.findOwnerUsername(replyId)
        .orElseThrow(() -> new NoSuchElementException("reply not found"));

    if (!owner.equals(username)) {
      throw new SecurityException("not owner");
    }

    int deleted = replyRepository.softDelete(replyId, LocalDateTime.now());
    if (deleted == 0) {
      throw new IllegalStateException("reply not deleted");
    }
  }

  private CommentDtos.UserProfile toProfile(Fp100User u, String fallbackUsername, Integer fallbackUserId) {
    CommentDtos.UserProfile p = new CommentDtos.UserProfile();
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
