package com.plateapp.plate_main.friend.service;

import com.plateapp.plate_main.friend.dto.FriendVisitCreateRequest;
import com.plateapp.plate_main.friend.dto.FriendVisitCreateResponse;
import com.plateapp.plate_main.friend.dto.FriendVisitDeleteResponse;
import com.plateapp.plate_main.friend.dto.FriendVisitUpdateRequest;
import com.plateapp.plate_main.friend.dto.FriendVisitUpdateResponse;
import com.plateapp.plate_main.friend.entity.Fp200Visit;
import com.plateapp.plate_main.friend.repository.Fp200VisitRepository;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendVisitCommandService {

    private final Fp200VisitRepository fp200VisitRepository;
    private final Fp300StoreRepository fp300StoreRepository;

    @Transactional
    public FriendVisitCreateResponse saveVisits(String username, FriendVisitCreateRequest req) {
        List<String> targets = req.getFriends() == null ? List.of() : req.getFriends();
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("friends is required");
        }

        StoreInfo storeInfo = resolveStoreInfo(req);
        List<Fp200Visit> rows = buildVisitRows(username, req, storeInfo, targets);
        List<Fp200Visit> saved = fp200VisitRepository.saveAll(rows);

        return FriendVisitCreateResponse.builder()
                .ok(true)
                .count(saved.size())
                .items(saved.stream()
                        .map(v -> FriendVisitCreateResponse.Item.builder()
                                .id(v.getId())
                                .username(v.getUsername())
                                .friendName(v.getFriendName())
                                .storeId(v.getStoreId())
                                .storeName(v.getStoreName())
                                .address(v.getAddress())
                                .memo(v.getMemo())
                                .visitDate(v.getVisitDate())
                                .createdAt(v.getCreatedAt())
                                .build())
                        .toList())
                .build();
    }

    @Transactional
    public FriendVisitUpdateResponse updateVisit(String username, Integer id, FriendVisitUpdateRequest req) {
        Fp200Visit visit = fp200VisitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("visit not found: " + id));
        if (!username.equals(visit.getUsername())) {
            throw new IllegalArgumentException("unauthorized visit update");
        }

        String storeName = normalize(req.getStoreName());
        String address = normalize(req.getAddress());

        if (storeName != null) {
            visit.setStoreName(storeName);
        }
        if (address != null) {
            visit.setAddress(address);
        }
        if (req.getMemo() != null) {
            visit.setMemo(req.getMemo());
        }
        visit.setVisitDate(req.getVisitDate());
        visit.setUpdatedAt(LocalDateTime.now());

        Fp200Visit saved = fp200VisitRepository.save(visit);

        return FriendVisitUpdateResponse.builder()
                .ok(true)
                .id(saved.getId())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Transactional
    public FriendVisitDeleteResponse deleteVisit(String username, Integer id) {
        Fp200Visit visit = fp200VisitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("visit not found: " + id));
        if (!username.equals(visit.getUsername())) {
            throw new IllegalArgumentException("unauthorized visit delete");
        }
        fp200VisitRepository.delete(visit);
        return FriendVisitDeleteResponse.builder()
                .ok(true)
                .id(id)
                .build();
    }

    private StoreInfo resolveStoreInfo(FriendVisitCreateRequest req) {
        Integer storeId = req.getStoreId();
        if (storeId == null) {
            String storeName = normalize(req.getStoreName());
            String address = normalize(req.getAddress());
            if (storeName == null || address == null) {
                throw new IllegalArgumentException("storeName and address are required when storeId is missing");
            }
            return new StoreInfo(null, storeName, address);
        }

        Fp300Store store = fp300StoreRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("store not found: " + storeId));

        String storeName = normalize(store.getStoreName());
        if (storeName == null) {
            storeName = normalize(store.getTitle());
        }
        if (storeName == null) {
            storeName = normalize(req.getStoreName());
        }
        if (storeName == null) {
            throw new IllegalArgumentException("storeName is required for visits");
        }

        String address = normalize(store.getAddress());
        if (address == null) {
            address = normalize(req.getAddress());
        }

        return new StoreInfo(storeId, storeName, address);
    }

    private List<Fp200Visit> buildVisitRows(
            String username,
            FriendVisitCreateRequest req,
            StoreInfo storeInfo,
            List<String> targets
    ) {
        List<Fp200Visit> rows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (String friend : targets) {
            Fp200Visit v = new Fp200Visit();
            v.setUsername(username);
            v.setFriendName(friend);
            v.setStoreId(storeInfo.storeId);
            v.setStoreName(storeInfo.storeName);
            v.setAddress(storeInfo.address);
            v.setMemo(req.getMemo());
            v.setFeedId(0L);
            v.setVisitDate(req.getVisitDate());
            v.setCreatedAt(now);
            v.setUpdatedAt(now);
            rows.add(v);
        }
        return rows;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class StoreInfo {
        private final Integer storeId;
        private final String storeName;
        private final String address;

        private StoreInfo(Integer storeId, String storeName, String address) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.address = address;
        }
    }
}
