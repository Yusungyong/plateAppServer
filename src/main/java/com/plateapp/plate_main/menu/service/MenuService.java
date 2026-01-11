package com.plateapp.plate_main.menu.service;

import com.plateapp.plate_main.menu.dto.MenuItemResponse;
import com.plateapp.plate_main.menu.entity.Fp320Menu;
import com.plateapp.plate_main.menu.repository.Fp320MenuRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final Fp320MenuRepository fp320MenuRepository;

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenus(String placeId, String storeName) {
        List<Fp320Menu> menus = fp320MenuRepository.findActiveMenus(placeId, storeName);
        return menus.stream()
                .map(this::toResponse)
                .toList();
    }

    private MenuItemResponse toResponse(Fp320Menu m) {
        return MenuItemResponse.builder()
                .itemId(m.getItemId())
                .storeId(m.getStoreId())
                .itemName(m.getItemName())
                .price(m.getPrice())
                .description(m.getDescription())
                .menuImage(m.getMenuImage())
                .menuTitle(m.getMenuTitle())
                .placeId(m.getPlaceId())
                .storeName(m.getStoreName())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .deletedAt(m.getDeletedAt())
                .build();
    }
}
