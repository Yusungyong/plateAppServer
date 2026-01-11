package com.plateapp.plate_main.menu.controller;

import com.plateapp.plate_main.menu.dto.MenuItemResponse;
import com.plateapp.plate_main.menu.service.MenuService;
import java.util.List;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> listMenus(
            @RequestParam(name = "placeId", required = false) String placeId,
            @RequestParam(name = "storeName", required = false) String storeName
    ) {
        String decodedPlaceId = decodeUtf8(placeId);
        String decodedStoreName = decodeUtf8(storeName);
        return ResponseEntity.ok(menuService.getMenus(decodedPlaceId, decodedStoreName));
    }

    private String decodeUtf8(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
