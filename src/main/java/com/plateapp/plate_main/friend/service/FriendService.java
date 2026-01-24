package com.plateapp.plate_main.friend.service;

import com.plateapp.plate_main.friend.dto.FriendDto;
import com.plateapp.plate_main.friend.dto.FriendRequests.CreateFriendRequest;
import com.plateapp.plate_main.friend.dto.FriendSearchDto;
import com.plateapp.plate_main.friend.dto.FriendVisitDtos.VisitItem;
import com.plateapp.plate_main.friend.dto.FriendVisitDtos.VisitResponse;
import com.plateapp.plate_main.friend.dto.FriendVisitDtos.FriendInfo;
import com.plateapp.plate_main.friend.dto.FriendVisitDtos.FriendVisitResponse;
import com.plateapp.plate_main.friend.dto.FriendRecentStoresDto.RecentStoreItem;
import com.plateapp.plate_main.friend.dto.FriendRecentStoresDto.RecentStoreResponse;
import com.plateapp.plate_main.friend.dto.FriendRecentStoresDto.FriendVisitSummary;
import com.plateapp.plate_main.friend.dto.FriendStoreVisitDto;
import com.plateapp.plate_main.friend.dto.FriendVisitDtos.ScheduledVisitItem;
import com.plateapp.plate_main.friend.dto.FriendVisitDtos.ScheduledVisitResponse;
import com.plateapp.plate_main.friend.entity.Fp150Friend;
import com.plateapp.plate_main.friend.repository.Fp150FriendRepository;
import com.plateapp.plate_main.friend.repository.Fp200VisitRepository;
import com.plateapp.plate_main.user.entity.Fp100User;
import com.plateapp.plate_main.user.repository.MemberRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendService {

    private final Fp150FriendRepository repository;
    private final Fp200VisitRepository visitRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<FriendDto> list(String username, String status) {
        log.debug("Listing friends: username={}, status={}", username, status);
        List<Fp150Friend> rows = (status == null || status.isBlank())
                ? repository.findByUsername(username)
                : repository.findByUsernameAndStatus(username, status);

        List<String> friendNames = rows.stream()
                .map(Fp150Friend::getFriendName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();

        Map<String, Fp100User> friendUserMap = friendNames.isEmpty()
                ? Map.of()
                : memberRepository.findByUsernameIn(friendNames).stream()
                .collect(Collectors.toMap(Fp100User::getUsername, u -> u, (a, b) -> a));

        List<FriendDto> result = rows.stream()
                .map(row -> toDto(row, friendUserMap.get(row.getFriendName())))
                .toList();

        log.debug("Found {} friends for username={}, status={}", result.size(), username, status);
        return result;
    }

    @Transactional
    public FriendDto add(CreateFriendRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.friendName() == null || request.friendName().isBlank()) {
            throw new IllegalArgumentException("username and friendName are required");
        }

        Fp150Friend friend = repository
                .findByUsernameAndFriendName(request.username(), request.friendName())
                .orElseGet(Fp150Friend::new);

        friend.setUsername(request.username());
        friend.setFriendName(request.friendName());
        friend.setStatus(request.status() != null && !request.status().isBlank()
                ? request.status()
                : (friend.getStatus() != null ? friend.getStatus() : "pending"));
        friend.setInitiatorUsername(request.initiatorUsername() != null && !request.initiatorUsername().isBlank()
                ? request.initiatorUsername()
                : request.username());
        friend.setMessage(request.message());

        Fp150Friend saved = repository.save(friend);
        Fp100User friendUser = memberRepository.findById(saved.getFriendName()).orElse(null);
        return toDto(saved, friendUser);
    }

    @Transactional
    public FriendDto updateStatus(Integer id, String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        Fp150Friend friend = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("friend not found: " + id));
        friend.setStatus(status);
        if ("cd_002".equalsIgnoreCase(status)) {
            friend.setAcceptedAt(java.time.LocalDateTime.now());
        }
        Fp150Friend saved = repository.save(friend);
        Fp100User friendUser = memberRepository.findById(saved.getFriendName()).orElse(null);
        return toDto(saved, friendUser);
    }

    @Transactional
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<FriendSearchDto> search(String keyword, int limit) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Fp100User> users = memberRepository.searchByKeyword(kw, PageRequest.of(0, safeLimit));
        return users.stream()
                .map(u -> FriendSearchDto.builder()
                        .username(u.getUsername())
                        .nickname(u.getNickName())
                        .profileImageUrl(u.getProfileImageUrl())
                        .activeRegion(u.getActiveRegion())
                        .mutualCount(0L) // TODO: compute mutuals
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public VisitResponse listVisits(String username, String friendName, Integer cursor, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<Fp200VisitRepository.VisitRow> rows = (friendName == null || friendName.isBlank())
                ? visitRepository.findVisits(username, cursor, safeLimit)
                : visitRepository.findFriendVisits(username, friendName, cursor, safeLimit);

        List<VisitItem> items = rows.stream()
                .map(r -> VisitItem.builder()
                        .id(r.getId())
                        .friendName(r.getFriendName())
                        .storeId(r.getStoreId())
                        .storeName(r.getStoreName())
                        .address(r.getAddress())
                        .memo(r.getMemo())
                        .visitDate(r.getVisitDate())
                        .thumbnail(r.getThumbnail())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        Integer nextCursor = (items.size() == safeLimit)
                ? items.get(items.size() - 1).getId()
                : null;

        return VisitResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .build();
    }

    @Transactional(readOnly = true)
    public RecentStoreResponse listRecentStores(String username, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        List<Fp200VisitRepository.RecentStoreRow> stores = visitRepository.findRecentStores(username, safeLimit);
        if (stores.isEmpty()) {
            return RecentStoreResponse.builder().items(List.of()).build();
        }

        List<Integer> storeIds = stores.stream()
                .map(Fp200VisitRepository.RecentStoreRow::getStoreId)
                .filter(Objects::nonNull)
                .toList();

        Map<Integer, List<Fp200VisitRepository.RecentFriendVisitRow>> byStore = visitRepository.findFriendVisitsForStores(username, storeIds).stream()
                .collect(Collectors.groupingBy(Fp200VisitRepository.RecentFriendVisitRow::getStoreId));

        List<RecentStoreItem> items = stores.stream()
                .map(s -> {
                    List<FriendVisitSummary> friends = byStore.getOrDefault(s.getStoreId(), List.of()).stream()
                            .limit(5)
                            .map(v -> FriendVisitSummary.builder()
                                    .friendName(v.getFriendName())
                                    .visitDate(v.getVisitDate())
                                    .build())
                            .toList();

                    return RecentStoreItem.builder()
                            .storeId(s.getStoreId())
                            .storeName(s.getStoreName())
                            .address(s.getAddress())
                            .placeId(s.getPlaceId())
                            .visitCount(s.getVisitCount())
                            .lastVisitedAt(s.getLastVisitedAt())
                            .friends(friends)
                            .thumbnail(s.getThumbnail())
                            .build();
                })
                .toList();

        return RecentStoreResponse.builder().items(items).build();
    }

    @Transactional(readOnly = true)
    public FriendStoreVisitDto getStoreFriendVisits(String username, Integer storeId) {
        List<Fp200VisitRepository.StoreFriendVisitRow> rows = visitRepository.findFriendVisitsByStore(username, storeId);
        if (rows.isEmpty()) {
            return FriendStoreVisitDto.builder()
                    .storeId(storeId)
                    .storeName(null)
                    .friends(List.of())
                    .build();
        }

        String storeName = rows.get(0).getStoreName();

        List<FriendStoreVisitDto.FriendVisit> friends = rows.stream()
                .map(r -> FriendStoreVisitDto.FriendVisit.builder()
                        .friendName(r.getFriendName())
                        .nickname(r.getNickname())
                        .profileImageUrl(r.getProfileImageUrl())
                        .memo(r.getMemo())
                        .visitDate(r.getVisitDate())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        return FriendStoreVisitDto.builder()
                .storeId(storeId)
                .storeName(storeName)
                .friends(friends)
                .build();
    }

    @Transactional(readOnly = true)
    public ScheduledVisitResponse listScheduledVisits(String username, LocalDate fromDate, LocalDate toDate) {
        List<Fp200VisitRepository.ScheduledVisitRow> rows = visitRepository.findScheduledVisits(username, fromDate, toDate);

        List<ScheduledVisitItem> items = rows.stream()
                .map(r -> ScheduledVisitItem.builder()
                        .id(r.getId())
                        .friendName(r.getFriendName())
                        .storeId(r.getStoreId())
                        .storeName(r.getStoreName())
                        .memo(r.getMemo())
                        .visitDate(r.getVisitDate())
                        .address(r.getAddress())
                        .thumbnail(r.getThumbnail())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        return ScheduledVisitResponse.builder()
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public FriendVisitResponse listFriendVisits(String username, String friendName, Integer cursor, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<Fp200VisitRepository.VisitRow> rows = visitRepository.findFriendVisits(username, friendName, cursor, safeLimit);

        List<VisitItem> items = rows.stream()
                .map(r -> VisitItem.builder()
                        .id(r.getId())
                        .friendName(r.getFriendName())
                        .storeId(r.getStoreId())
                        .storeName(r.getStoreName())
                        .address(r.getAddress())
                        .memo(r.getMemo())
                        .visitDate(r.getVisitDate())
                        .thumbnail(r.getThumbnail())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        Integer nextCursor = (items.size() == safeLimit)
                ? items.get(items.size() - 1).getId()
                : null;

        Fp100User f = memberRepository.findById(friendName).orElse(null);
        FriendInfo info = FriendInfo.builder()
                .username(friendName)
                .nickname(f != null ? f.getNickName() : null)
                .profileImageUrl(f != null ? f.getProfileImageUrl() : null)
                .build();

        return FriendVisitResponse.builder()
                .friend(info)
                .items(items)
                .nextCursor(nextCursor)
                .build();
    }

    @Transactional(readOnly = true)
    public List<FriendDto> suggest(String username, String keyword, String status, int limit, int offset) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        int safeOffset = Math.max(0, offset);
        String statusFilter = (status == null || status.isBlank()) ? "cd_002" : status;

        return repository.suggestFriends(username, kw, statusFilter, safeLimit, safeOffset)
                .stream()
                .map(r -> FriendDto.builder()
                        .id(r.getId())
                        .username(r.getUsername())
                        .friendName(r.getFriendName())
                        .friendNickname(r.getFriendNickname())
                        .status(r.getStatus())
                        .friendProfileImageUrl(r.getFriendProfileImageUrl())
                        .friendActiveRegion(r.getFriendActiveRegion())
                        .initiatorUsername(r.getInitiatorUsername())
                        .message(r.getMessage())
                        .mutualCount(0L)
                        .createdAt(r.getCreatedAt())
                        .updatedAt(r.getUpdatedAt())
                        .acceptedAt(r.getAcceptedAt())
                        .build())
                .toList();
    }

    private FriendDto toDto(Fp150Friend e, Fp100User friendUser) {
        String nickname = null;
        String profileImage = null;
        String activeRegion = null;
        if (friendUser != null) {
            nickname = friendUser.getNickName();
            profileImage = friendUser.getProfileImageUrl();
            activeRegion = friendUser.getActiveRegion();
        }
        if (nickname == null || nickname.isBlank()) {
            nickname = e.getFriendName(); // fallback
        }

        return FriendDto.builder()
                .id(e.getId())
                .username(e.getUsername())
                .friendName(e.getFriendName())
                .friendNickname(nickname)
                .status(e.getStatus())
                .friendProfileImageUrl(profileImage)
                .friendActiveRegion(activeRegion)
                .initiatorUsername(e.getInitiatorUsername())
                .message(e.getMessage())
                .mutualCount(0L) // TODO: calculate mutual friends if needed
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .acceptedAt(e.getAcceptedAt())
                .build();
    }
}
