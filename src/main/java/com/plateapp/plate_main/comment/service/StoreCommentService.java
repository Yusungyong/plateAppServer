package com.plateapp.plate_main.comment.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.comment.dto.*;
import com.plateapp.plate_main.comment.entity.Fp440Comment;
import com.plateapp.plate_main.comment.entity.Fp450Reply;
import com.plateapp.plate_main.comment.repository.CommentRepository;
import com.plateapp.plate_main.comment.repository.ReplyRepository;
import com.plateapp.plate_main.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreCommentService {

    private final CommentRepository commentRepository;
    private final ReplyRepository replyRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentDTO createComment(Integer storeId, String username, CommentCreateRequest request) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }

        Fp440Comment comment = new Fp440Comment();
        comment.setStoreId(storeId);
        comment.setUsername(username);
        comment.setUserId(userId);
        comment.setContent(request.getContent());

        Fp440Comment saved = commentRepository.save(comment);

        // TODO: 알림 생성

        return toCommentDTO(saved, username);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CommentDTO> getComments(Integer storeId, String currentUsername, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Fp440Comment> page = commentRepository.findByStoreIdAndUseYn(storeId, "Y", pageable);

        List<CommentDTO> comments = page.getContent().stream()
                .map(comment -> toCommentDTO(comment, currentUsername))
                .collect(Collectors.toList());

        return PagedResponse.of(comments, page.getTotalElements(), limit, offset);
    }

    @Transactional
    public CommentDTO updateComment(Integer storeId, Integer commentId, String username, CommentUpdateRequest request) {
        Fp440Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }

        comment.setContent(request.getContent());
        Fp440Comment updated = commentRepository.save(comment);

        return toCommentDTO(updated, username);
    }

    @Transactional
    public void deleteComment(Integer storeId, Integer commentId, String username) {
        Fp440Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }

        // 답글도 함께 삭제
        replyRepository.deleteByCommentId(commentId);
        commentRepository.delete(comment);
    }

    @Transactional
    public ReplyDTO createReply(Integer storeId, Integer commentId, String username, CommentCreateRequest request) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }

        Fp450Reply reply = new Fp450Reply();
        reply.setCommentId(commentId);
        reply.setUsername(username);
        reply.setContent(request.getContent());

        Fp450Reply saved = replyRepository.save(reply);

        // TODO: 알림 생성

        return toReplyDTO(saved, username);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReplyDTO> getReplies(Integer storeId, Integer commentId, String currentUsername, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Fp450Reply> page = replyRepository.findByCommentIdAndUseYn(commentId, "Y", pageable);

        List<ReplyDTO> replies = page.getContent().stream()
                .map(reply -> toReplyDTO(reply, currentUsername))
                .collect(Collectors.toList());

        return PagedResponse.of(replies, page.getTotalElements(), limit, offset);
    }

    @Transactional
    public ReplyDTO updateReply(Integer storeId, Integer commentId, Integer replyId, String username, CommentUpdateRequest request) {
        Fp450Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Reply not found"));

        if (!reply.getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }

        reply.setContent(request.getContent());
        Fp450Reply updated = replyRepository.save(reply);

        return toReplyDTO(updated, username);
    }

    @Transactional
    public void deleteReply(Integer storeId, Integer commentId, Integer replyId, String username) {
        Fp450Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Reply not found"));

        if (!reply.getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }

        replyRepository.delete(reply);
    }

    private CommentDTO toCommentDTO(Fp440Comment comment, String currentUsername) {
        Optional<User> userOpt = userRepository.findById(comment.getUsername());
        long replyCount = replyRepository.countByCommentIdAndUseYn(comment.getCommentId(), "Y");

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return CommentDTO.builder()
                    .commentId(comment.getCommentId().longValue())
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .profileImageUrl(user.getProfileImageUrl())
                    .content(comment.getContent())
                    .createdAt(comment.getCreatedAt())
                    .updatedAt(comment.getUpdatedAt())
                    .replyCount(replyCount)
                    .isOwner(user.getUsername().equals(currentUsername))
                    .build();
        }

        return null;
    }

    private ReplyDTO toReplyDTO(Fp450Reply reply, String currentUsername) {
        Optional<User> userOpt = userRepository.findById(reply.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ReplyDTO.builder()
                    .replyId(reply.getReplyId().longValue())
                    .commentId(reply.getCommentId().longValue())
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .profileImageUrl(user.getProfileImageUrl())
                    .content(reply.getContent())
                    .createdAt(reply.getCreatedAt())
                    .updatedAt(reply.getUpdatedAt())
                    .isOwner(user.getUsername().equals(currentUsername))
                    .build();
        }

        return null;
    }
}
