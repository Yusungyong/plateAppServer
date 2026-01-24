package com.plateapp.plate_main.comment.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.comment.dto.*;
import com.plateapp.plate_main.comment.entity.ImageComment;
import com.plateapp.plate_main.comment.entity.ImageReply;
import com.plateapp.plate_main.comment.repository.ImageCommentRepository;
import com.plateapp.plate_main.comment.repository.ImageReplyRepository;
import com.plateapp.plate_main.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageCommentService {

    private final ImageCommentRepository commentRepository;
    private final ImageReplyRepository replyRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentDTO createComment(Integer feedId, String username, CommentCreateRequest request) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }

        ImageComment comment = ImageComment.builder()
                .imageFeedId(feedId)
                .userId(userId)
                .username(username)
                .content(request.getContent())
                .build();

        ImageComment saved = commentRepository.save(comment);

        // TODO: 알림 생성

        return toCommentDTO(saved, username);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CommentDTO> getComments(Integer feedId, String currentUsername, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ImageComment> page = commentRepository.findByImageFeedIdAndUseYn(feedId, "Y", pageable);

        List<CommentDTO> comments = page.getContent().stream()
                .map(comment -> toCommentDTO(comment, currentUsername))
                .collect(Collectors.toList());

        return PagedResponse.of(comments, page.getTotalElements(), limit, offset);
    }

    @Transactional
    public CommentDTO updateComment(Integer feedId, Integer commentId, String username, CommentUpdateRequest request) {
        ImageComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }

        comment.setContent(request.getContent());
        ImageComment updated = commentRepository.save(comment);

        return toCommentDTO(updated, username);
    }

    @Transactional
    public void deleteComment(Integer feedId, Integer commentId, String username) {
        ImageComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }

        // Soft delete
        comment.setUseYn("N");
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);

        // Delete all replies (cascade)
        replyRepository.deleteByCommentId(commentId);
    }

    @Transactional
    public ReplyDTO createReply(Integer feedId, Integer commentId, String username, CommentCreateRequest request) {
        // Verify comment exists
        commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        ImageReply reply = ImageReply.builder()
                .commentId(commentId)
                .username(username)
                .content(request.getContent())
                .build();

        ImageReply saved = replyRepository.save(reply);

        // TODO: 알림 생성

        return toReplyDTO(saved, username);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReplyDTO> getReplies(Integer feedId, Integer commentId, String currentUsername, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ImageReply> page = replyRepository.findByCommentIdAndUseYn(commentId, "Y", pageable);

        List<ReplyDTO> replies = page.getContent().stream()
                .map(reply -> toReplyDTO(reply, currentUsername))
                .collect(Collectors.toList());

        return PagedResponse.of(replies, page.getTotalElements(), limit, offset);
    }

    @Transactional
    public ReplyDTO updateReply(Integer feedId, Integer commentId, Integer replyId, String username, CommentUpdateRequest request) {
        ImageReply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Reply not found"));

        if (!reply.getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }

        reply.setContent(request.getContent());
        ImageReply updated = replyRepository.save(reply);

        return toReplyDTO(updated, username);
    }

    @Transactional
    public void deleteReply(Integer feedId, Integer commentId, Integer replyId, String username) {
        ImageReply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Reply not found"));

        if (!reply.getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }

        // Soft delete
        reply.setUseYn("N");
        reply.setDeletedAt(LocalDateTime.now());
        replyRepository.save(reply);
    }

    private CommentDTO toCommentDTO(ImageComment comment, String currentUsername) {
        Optional<User> userOpt = userRepository.findById(comment.getUsername());

        long replyCount = replyRepository.countByCommentIdAndUseYn(comment.getCommentId(), "Y");

        CommentDTO.CommentDTOBuilder builder = CommentDTO.builder()
                .commentId(comment.getCommentId().longValue())
                .username(comment.getUsername())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replyCount((int) replyCount)
                .isOwner(comment.getUsername().equals(currentUsername));

        userOpt.ifPresent(user -> {
            builder.nickname(user.getNickname());
            builder.profileImageUrl(user.getProfileImageUrl());
        });

        return builder.build();
    }

    private ReplyDTO toReplyDTO(ImageReply reply, String currentUsername) {
        Optional<User> userOpt = userRepository.findById(reply.getUsername());

        ReplyDTO.ReplyDTOBuilder builder = ReplyDTO.builder()
                .replyId(reply.getReplyId().longValue())
                .commentId(reply.getCommentId().longValue())
                .username(reply.getUsername())
                .content(reply.getContent())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .isOwner(reply.getUsername().equals(currentUsername));

        userOpt.ifPresent(user -> {
            builder.nickname(user.getNickname());
            builder.profileImageUrl(user.getProfileImageUrl());
        });

        return builder.build();
    }
}
