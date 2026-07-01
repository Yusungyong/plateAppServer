package com.plateapp.plate_main.common.config;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SupportPolicyController {

    private static final Map<String, List<CodeItem>> CODE_GROUPS = codeGroups();

    @GetMapping("/api/support/policies")
    public ApiResponse<SupportPoliciesResponse> policies() {
        return ApiResponse.ok(new SupportPoliciesResponse(
                "평일 10:00-18:00",
                "보통 1-2영업일 안에 답변합니다.",
                "개인 확인이 필요한 문의는 최대 3영업일이 걸릴 수 있습니다."
        ));
    }

    @GetMapping("/api/common/codes")
    public ApiResponse<CodeGroupsResponse> codes(
            @RequestParam(required = false) String groups
    ) {
        Map<String, List<CodeItem>> selected = new LinkedHashMap<>();
        if (groups == null || groups.isBlank()) {
            selected.putAll(CODE_GROUPS);
        } else {
            Arrays.stream(groups.split(","))
                    .map(String::trim)
                    .filter(group -> !group.isEmpty())
                    .forEach(group -> {
                        List<CodeItem> items = CODE_GROUPS.get(group);
                        if (items == null) {
                            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Unsupported code group.");
                        }
                        selected.put(group, items);
                    });
        }
        return ApiResponse.ok(new CodeGroupsResponse(selected));
    }

    private static Map<String, List<CodeItem>> codeGroups() {
        Map<String, List<CodeItem>> groups = new LinkedHashMap<>();
        groups.put("qnaCategory", List.of(
                new CodeItem("account", "계정문의"),
                new CodeItem("storeApproval", "입점문의"),
                new CodeItem("storeOperation", "매장관리"),
                new CodeItem("service", "서비스문의"),
                new CodeItem("other", "기타")
        ));
        groups.put("qnaStatus", List.of(
                new CodeItem("received", "접수"),
                new CodeItem("reviewing", "검토 중"),
                new CodeItem("answered", "답변 완료"),
                new CodeItem("hidden", "숨김")
        ));
        groups.put("reviewReason", List.of(
                new CodeItem("MISSING_DOCUMENT", "필수 서류 누락"),
                new CodeItem("INVALID_DOCUMENT", "유효하지 않은 서류"),
                new CodeItem("BUSINESS_INFO_MISMATCH", "사업자 정보 불일치"),
                new CodeItem("DUPLICATE_STORE", "중복 매장"),
                new CodeItem("UNSUPPORTED_BUSINESS", "지원하지 않는 업종"),
                new CodeItem("OTHER", "기타")
        ));
        groups.put("changeRequestReason", List.of(
                new CodeItem("OWNER_CONTACT_INVALID", "담당자 연락처 확인 필요"),
                new CodeItem("INVALID_PHONE", "연락처 형식 오류"),
                new CodeItem("ADDRESS_UNCLEAR", "주소 확인 필요"),
                new CodeItem("BUSINESS_INFO_MISMATCH", "사업자 정보 확인 필요"),
                new CodeItem("MENU_INFO_INCOMPLETE", "메뉴 정보 보완 필요"),
                new CodeItem("OTHER", "기타")
        ));
        return Collections.unmodifiableMap(groups);
    }

    public record SupportPoliciesResponse(
            String supportHours,
            String qnaExpectedResponse,
            String privateInquiryExpectedResponse
    ) {
    }

    public record CodeGroupsResponse(Map<String, List<CodeItem>> groups) {
    }

    public record CodeItem(String code, String name) {
    }
}
