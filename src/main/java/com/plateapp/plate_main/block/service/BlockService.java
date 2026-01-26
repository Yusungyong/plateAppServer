package com.plateapp.plate_main.block.service;

import com.plateapp.plate_main.block.dto.BlockCreateRequest;
import com.plateapp.plate_main.block.dto.BlockedUser;
import com.plateapp.plate_main.block.dto.BlockedUsersResponse;
import com.plateapp.plate_main.block.entity.Fp160Block;
import com.plateapp.plate_main.block.repository.BlockRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.user.entity.Fp100User;
import com.plateapp.plate_main.user.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createBlock(String blockerUsername, BlockCreateRequest request) {
        if (blockerUsername == null || blockerUsername.isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthenticated");
        }
        String blockedUsername = request.getBlockedUsername().trim();
        if (blockedUsername.isBlank()) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "blockedUsername required");
        }
        if (blockerUsername.equals(blockedUsername)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "cannot block self");
        }
        if (blockRepository.existsByBlockerUsernameAndBlockedUsername(blockerUsername, blockedUsername)) {
            return;
        }

        Fp160Block block = new Fp160Block();
        block.setBlockerUsername(blockerUsername);
        block.setBlockedUsername(blockedUsername);
        block.setBlockedAt(LocalDateTime.now());
        blockRepository.save(block);
    }

    @Transactional
    public void deleteBlock(String blockerUsername, String blockedUsername) {
        if (blockerUsername == null || blockerUsername.isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthenticated");
        }
        if (blockedUsername == null || blockedUsername.isBlank()) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "blockedUsername required");
        }
        blockRepository.deleteByBlockerUsernameAndBlockedUsername(blockerUsername, blockedUsername);
    }

    @Transactional(readOnly = true)
    public BlockedUsersResponse listBlocks(String blockerUsername, int limit, int offset) {
        if (blockerUsername == null || blockerUsername.isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthenticated");
        }

        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(offset, 0);

        PageRequest pageable = PageRequest.of(
                safeOffset / safeLimit,
                safeLimit,
                Sort.by(Sort.Direction.DESC, "blockedAt")
        );
        Page<Fp160Block> page = blockRepository.findByBlockerUsername(blockerUsername, pageable);
        List<Fp160Block> blocks = page.getContent();

        List<String> usernames = new ArrayList<>();
        for (Fp160Block block : blocks) {
            usernames.add(block.getBlockedUsername());
        }

        Map<String, Fp100User> usersByUsername = new HashMap<>();
        if (!usernames.isEmpty()) {
            for (Fp100User user : memberRepository.findByUsernameIn(usernames)) {
                usersByUsername.put(user.getUsername(), user);
            }
        }

        List<BlockedUser> items = new ArrayList<>();
        for (Fp160Block block : blocks) {
            Fp100User user = usersByUsername.get(block.getBlockedUsername());
            items.add(BlockedUser.builder()
                    .blockedUsername(block.getBlockedUsername())
                    .blockedNickname(user != null ? user.getNickName() : null)
                    .blockedProfileImageUrl(user != null ? user.getProfileImageUrl() : null)
                    .blockedAt(block.getBlockedAt())
                    .blockedUserId(user != null ? user.getUserId() : null)
                    .blockedActiveRegion(user != null ? user.getActiveRegion() : null)
                    .build());
        }

        items.sort(Comparator.comparing(BlockedUser::getBlockedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return BlockedUsersResponse.builder()
                .items(items)
                .total(page.getTotalElements())
                .offset(safeOffset)
                .limit(safeLimit)
                .build();
    }
}
