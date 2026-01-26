package com.plateapp.plate_main.block.controller;

import com.plateapp.plate_main.block.dto.BlockCreateRequest;
import com.plateapp.plate_main.block.dto.BlockedUsersResponse;
import com.plateapp.plate_main.block.service.BlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @PostMapping("/blocks")
    public ResponseEntity<?> createBlock(@RequestBody @Valid BlockCreateRequest request) {
        String username = currentUsername();
        blockService.createBlock(username, request);
        return ResponseEntity.ok(java.util.Map.of("ok", true));
    }

    @GetMapping("/blocks")
    public ResponseEntity<BlockedUsersResponse> listBlocks(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        String username = currentUsername();
        BlockedUsersResponse response = blockService.listBlocks(username, limit, offset);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/blocks/{blockedUsername}")
    public ResponseEntity<?> deleteBlock(@PathVariable String blockedUsername) {
        String username = currentUsername();
        blockService.deleteBlock(username, blockedUsername);
        return ResponseEntity.ok(java.util.Map.of("ok", true));
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String name = auth.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
                return name;
            }
        }
        return null;
    }
}
