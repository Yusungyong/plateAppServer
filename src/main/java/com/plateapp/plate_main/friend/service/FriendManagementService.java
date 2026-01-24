package com.plateapp.plate_main.friend.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.dto.PagedResponse;
import com.plateapp.plate_main.friend.dto.*;
import com.plateapp.plate_main.friend.entity.Fp150Friend;
import com.plateapp.plate_main.friend.repository.Fp150FriendRepository;
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
public class FriendManagementService {

    private final Fp150FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<FriendDTO> getFriends(String username, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Fp150Friend> page = friendRepository.findByUsernameAndStatus(username, "accepted", pageable);

        List<FriendDTO> friends = page.getContent().stream()
                .map(friend -> {
                    Optional<User> userOpt = userRepository.findById(friend.getFriendName());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        return FriendDTO.builder()
                                .userId(user.getUserId())
                                .username(user.getUsername())
                                .nickname(user.getNickname())
                                .profileImageUrl(user.getProfileImageUrl())
                                .activeRegion(user.getActiveRegion())
                                .friendsSince(friend.getAcceptedAt())
                                .build();
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return PagedResponse.of(friends, page.getTotalElements(), limit, offset);
    }

    @Transactional(readOnly = true)
    public PagedResponse<FriendSearchResultDTO> searchUsers(String query, String currentUsername, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<User> page = userRepository.findByUsernameContainingOrNicknameContaining(query, query, pageable);

        List<FriendSearchResultDTO> results = page.getContent().stream()
                .map(user -> {
                    boolean isFriend = friendRepository.existsByUsernameAndFriendNameAndStatus(currentUsername, user.getUsername(), "accepted");
                    // Check if there's a pending request in either direction
                    boolean isPending = friendRepository.existsByUsernameAndFriendNameAndStatus(currentUsername, user.getUsername(), "pending") ||
                                       friendRepository.existsByUsernameAndFriendNameAndStatus(user.getUsername(), currentUsername, "pending");

                    return FriendSearchResultDTO.builder()
                            .userId(user.getUserId())
                            .username(user.getUsername())
                            .nickname(user.getNickname())
                            .profileImageUrl(user.getProfileImageUrl())
                            .activeRegion(user.getActiveRegion())
                            .isFriend(isFriend)
                            .isPending(isPending)
                            .build();
                })
                .collect(Collectors.toList());

        return PagedResponse.of(results, page.getTotalElements(), limit, offset);
    }

    @Transactional
    public void deleteFriend(String username, Integer friendUserId) {
        User friendUser = userRepository.findByUserId(friendUserId)
                .orElseThrow(() -> new IllegalArgumentException("Friend user not found"));

        // 양방향 삭제
        friendRepository.deleteByUsernameAndFriendName(username, friendUser.getUsername());
        friendRepository.deleteByUsernameAndFriendName(friendUser.getUsername(), username);
    }

    @Transactional(readOnly = true)
    public PagedResponse<FriendRequestDTO> getSentRequests(String username, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        // Sent requests: where I'm the initiator and status is pending
        Page<Fp150Friend> page = friendRepository.findByUsernameAndStatus(username, "pending", pageable);

        List<FriendRequestDTO> requests = page.getContent().stream()
                .filter(friend -> username.equals(friend.getInitiatorUsername())) // Only requests I initiated
                .map(friend -> toFriendRequestDTO(friend, username))
                .collect(Collectors.toList());

        return PagedResponse.of(requests, (long) requests.size(), limit, offset);
    }

    @Transactional(readOnly = true)
    public PagedResponse<FriendRequestDTO> getReceivedRequests(String username, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        // Received requests: where I'm the username (receiver) and someone else initiated
        Page<Fp150Friend> page = friendRepository.findByUsernameAndStatus(username, "pending", pageable);

        List<FriendRequestDTO> requests = page.getContent().stream()
                .filter(friend -> !username.equals(friend.getInitiatorUsername())) // Only requests others initiated to me
                .map(friend -> toFriendRequestDTO(friend, username))
                .collect(Collectors.toList());

        return PagedResponse.of(requests, (long) requests.size(), limit, offset);
    }

    @Transactional
    public FriendRequestDTO sendFriendRequest(String fromUsername, Integer toUserId) {
        Integer fromUserId = userRepository.findUserIdByUsername(fromUsername);
        if (fromUserId == null) {
            throw new IllegalArgumentException("User not found");
        }

        // 자기 자신에게 요청 불가
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }

        // 이미 친구인지 확인
        User toUser = userRepository.findByUserId(toUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        boolean alreadyFriends = friendRepository.existsByUsernameAndFriendNameAndStatus(fromUsername, toUser.getUsername(), "accepted");
        if (alreadyFriends) {
            throw new IllegalArgumentException("Already friends");
        }

        // 대기 중인 요청이 있는지 확인 (양방향)
        boolean pendingExists = friendRepository.existsByUsernameAndFriendNameAndStatus(fromUsername, toUser.getUsername(), "pending");
        if (pendingExists) {
            throw new IllegalArgumentException("Friend request already pending");
        }

        // Create pending friend request
        Fp150Friend request = new Fp150Friend();
        request.setUsername(fromUsername);
        request.setFriendName(toUser.getUsername());
        request.setStatus("pending");
        request.setInitiatorUsername(fromUsername);

        Fp150Friend saved = friendRepository.save(request);

        return toFriendRequestDTO(saved, fromUsername);
    }

    @Transactional
    public void cancelFriendRequest(Integer requestId, String username) {
        Fp150Friend request = friendRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        // Only the initiator can cancel
        if (!username.equals(request.getInitiatorUsername())) {
            throw new IllegalArgumentException("Not authorized");
        }

        if (!"pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("Can only cancel pending requests");
        }

        friendRepository.delete(request);
    }

    @Transactional
    public void acceptFriendRequest(Integer requestId, String username) {
        Fp150Friend request = friendRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        // Only the receiver can accept
        if (!username.equals(request.getUsername())) {
            throw new IllegalArgumentException("Not authorized");
        }

        if (!"pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("Request is not pending");
        }

        // Update request to accepted
        request.setStatus("accepted");
        request.setAcceptedAt(LocalDateTime.now());
        friendRepository.save(request);

        // Create reciprocal friendship
        Fp150Friend reciprocal = new Fp150Friend();
        reciprocal.setUsername(request.getFriendName());
        reciprocal.setFriendName(request.getUsername());
        reciprocal.setStatus("accepted");
        reciprocal.setInitiatorUsername(request.getInitiatorUsername());
        reciprocal.setAcceptedAt(LocalDateTime.now());
        friendRepository.save(reciprocal);

        // TODO: 알림 생성
    }

    @Transactional
    public void rejectFriendRequest(Integer requestId, String username) {
        Fp150Friend request = friendRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        // Only the receiver can reject
        if (!username.equals(request.getUsername())) {
            throw new IllegalArgumentException("Not authorized");
        }

        if (!"pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("Request is not pending");
        }

        // Just delete the request (soft reject)
        friendRepository.delete(request);
    }

    private FriendRequestDTO toFriendRequestDTO(Fp150Friend friend, String currentUsername) {
        // Determine who is from and who is to based on current user
        String fromUsername = friend.getInitiatorUsername();
        String toUsername = friend.getUsername();

        Optional<User> fromUserOpt = userRepository.findById(fromUsername);
        Optional<User> toUserOpt = userRepository.findById(toUsername);

        FriendRequestDTO.FriendRequestDTOBuilder builder = FriendRequestDTO.builder()
                .requestId(friend.getId().longValue())
                .status(friend.getStatus().toUpperCase())
                .createdAt(friend.getCreatedAt())
                .respondedAt(friend.getAcceptedAt());

        fromUserOpt.ifPresent(user -> {
            builder.fromUserId(user.getUserId());
            builder.fromUsername(user.getUsername());
            builder.fromNickname(user.getNickname());
            builder.fromProfileImageUrl(user.getProfileImageUrl());
        });

        toUserOpt.ifPresent(user -> {
            builder.toUserId(user.getUserId());
            builder.toUsername(user.getUsername());
        });

        return builder.build();
    }
}
