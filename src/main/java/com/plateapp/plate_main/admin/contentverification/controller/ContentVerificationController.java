package com.plateapp.plate_main.admin.contentverification.controller;
import com.plateapp.plate_main.admin.common.AdminPageResponse;
import com.plateapp.plate_main.admin.contentverification.dto.ContentVerificationDtos.*;
import com.plateapp.plate_main.admin.contentverification.service.ContentVerificationService;
import com.plateapp.plate_main.admin.security.*;
import com.plateapp.plate_main.auth.security.PlateAuthorities;
import com.plateapp.plate_main.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/content-verifications") @RequiredArgsConstructor
public class ContentVerificationController {
 private final ContentVerificationService service; private final AdminActorResolver actors;
 @GetMapping public ApiResponse<AdminPageResponse<Response>> list(Authentication a,@RequestParam(defaultValue="0")int page,
  @RequestParam(defaultValue="20")int size,@RequestParam(required=false)String status,@RequestParam(required=false)String targetType,
  @RequestParam(required=false)Integer assigneeUserId,@RequestParam(required=false)String keyword){read(a);return ApiResponse.ok(service.list(page,size,status,targetType,assigneeUserId,keyword));}
 @GetMapping("/{id}") public ApiResponse<Response> detail(@PathVariable Long id,Authentication a){read(a);return ApiResponse.ok(service.detail(id));}
 @GetMapping("/{id}/history") public ApiResponse<List<HistoryResponse>> history(@PathVariable Long id,Authentication a){read(a);return ApiResponse.ok(service.history(id));}
 @PatchMapping("/{id}/assignee") public ApiResponse<Response> assign(@PathVariable Long id,@Valid @RequestBody AssigneeRequest c,Authentication a,HttpServletRequest r){write(a);return ApiResponse.ok(service.assign(id,c,actors.resolve(a),r));}
 @PostMapping("/{id}/approve") public ApiResponse<Response> approve(@PathVariable Long id,@Valid @RequestBody ActionRequest c,Authentication a,HttpServletRequest r){write(a);return ApiResponse.ok(service.decide(id,c,"APPROVED",actors.resolve(a),r));}
 @PostMapping("/{id}/reject") public ApiResponse<Response> reject(@PathVariable Long id,@Valid @RequestBody ActionRequest c,Authentication a,HttpServletRequest r){write(a);return ApiResponse.ok(service.decide(id,c,"REJECTED",actors.resolve(a),r));}
 @PostMapping("/{id}/request-changes") public ApiResponse<Response> changes(@PathVariable Long id,@Valid @RequestBody ActionRequest c,Authentication a,HttpServletRequest r){write(a);return ApiResponse.ok(service.decide(id,c,"CHANGES_REQUESTED",actors.resolve(a),r));}
 private void read(Authentication a){if(!PlateAuthorities.hasAdminPermission(a,PlateAuthorities.PERMISSION_FEED_READ))throw new AccessDeniedException("FEED_READ required");}
 private void write(Authentication a){if(!PlateAuthorities.hasAdminPermission(a,PlateAuthorities.PERMISSION_FEED_MODERATE))throw new AccessDeniedException("FEED_MODERATE required");}
}
