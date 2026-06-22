package com.plateapp.plate_main.admin.storeoperation.controller;
import com.plateapp.plate_main.admin.common.AdminPageResponse;
import com.plateapp.plate_main.admin.security.*;
import com.plateapp.plate_main.admin.storeoperation.dto.AdminStoreDtos.*;
import com.plateapp.plate_main.admin.storeoperation.service.AdminStoreService;
import com.plateapp.plate_main.auth.security.PlateAuthorities;
import com.plateapp.plate_main.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/stores") @RequiredArgsConstructor
public class AdminStoreController {
 private final AdminStoreService service;private final AdminActorResolver actors;
 @GetMapping public ApiResponse<AdminPageResponse<Response>> list(Authentication a,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(required=false)String keyword,@RequestParam(required=false)String operationStatus,@RequestParam(required=false)String visibilityStatus){read(a);return ApiResponse.ok(service.list(page,size,keyword,operationStatus,visibilityStatus));}
 @GetMapping("/{id}") public ApiResponse<Response> detail(@PathVariable Long id,Authentication a){read(a);return ApiResponse.ok(service.detail(id));}
 @GetMapping("/{id}/history") public ApiResponse<List<HistoryResponse>> history(@PathVariable Long id,Authentication a){read(a);return ApiResponse.ok(service.history(id));}
 @PatchMapping("/{id}/operation-status") public ApiResponse<Response> operation(@PathVariable Long id,@Valid @RequestBody StatusRequest c,Authentication a,HttpServletRequest r){write(a);return ApiResponse.ok(service.operation(id,c,actors.resolve(a),r));}
 @PatchMapping("/{id}/visibility") public ApiResponse<Response> visibility(@PathVariable Long id,@Valid @RequestBody StatusRequest c,Authentication a,HttpServletRequest r){write(a);return ApiResponse.ok(service.visibility(id,c,actors.resolve(a),r));}
 private void read(Authentication a){if(!PlateAuthorities.hasAdminPermission(a,PlateAuthorities.PERMISSION_STORE_READ))throw new AccessDeniedException("STORE_READ required");}
 private void write(Authentication a){if(!PlateAuthorities.hasAdminPermission(a,PlateAuthorities.PERMISSION_STORE_UPDATE))throw new AccessDeniedException("STORE_UPDATE required");}
}
