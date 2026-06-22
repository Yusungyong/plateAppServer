package com.plateapp.plate_main.admin.storeoperation.service;

import com.plateapp.plate_main.admin.audit.repository.AdminAuditLogRepository;
import com.plateapp.plate_main.admin.audit.service.AdminAuditService;
import com.plateapp.plate_main.admin.common.AdminPageResponse;
import com.plateapp.plate_main.admin.security.AdminActor;
import com.plateapp.plate_main.admin.storeoperation.dto.AdminStoreDtos.HistoryResponse;
import com.plateapp.plate_main.admin.storeoperation.dto.AdminStoreDtos.Response;
import com.plateapp.plate_main.admin.storeoperation.dto.AdminStoreDtos.StatusRequest;
import com.plateapp.plate_main.admin.storeoperation.entity.AdminStoreOperation;
import com.plateapp.plate_main.admin.storeoperation.repository.AdminStoreOperationRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.restaurant.entity.Restaurant;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStoreService {
    private static final Set<String> OPERATIONS = Set.of("operating", "temporarily_closed", "closed");
    private static final Set<String> VISIBILITIES = Set.of("visible", "hidden");

    private final RestaurantRepository restaurantRepository;
    private final AdminStoreOperationRepository operationRepository;
    private final AdminAuditService audit;
    private final AdminAuditLogRepository auditRepository;

    @Transactional(readOnly = true)
    public AdminPageResponse<Response> list(
            int page,
            int size,
            String keyword,
            String operation,
            String visibility
    ) {
        validatePage(page, size);
        operation = normalize(operation);
        visibility = normalize(visibility);
        if (operation != null && !OPERATIONS.contains(operation)) throw invalid("Unsupported operationStatus.");
        if (visibility != null && !VISIBILITIES.contains(visibility)) throw invalid("Unsupported visibilityStatus.");
        return AdminPageResponse.from(operationRepository.searchStores(
                trim(keyword), operation, visibility,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt", "id"))
        ).map(this::map));
    }

    @Transactional(readOnly = true)
    public Response detail(Long id) {
        return map(findStore(id));
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(Long id) {
        findStore(id);
        return auditRepository.findByResourceTypeAndResourceIdOrderByOccurredAtDescIdDesc(
                "STORE", String.valueOf(id)
        ).stream().map(a -> new HistoryResponse(
                a.getId(), a.getOccurredAt(), a.getActorUserId(), a.getActorRole(), a.getAction(),
                a.getPreviousValue(), a.getNextValue(), a.getReason()
        )).toList();
    }

    @Transactional
    public Response operation(Long id, StatusRequest command, AdminActor actor, HttpServletRequest request) {
        Restaurant store = findStore(id);
        AdminStoreOperation state = stateForUpdate(store, command.version());
        String status = normalize(command.status());
        if (!OPERATIONS.contains(status)) throw invalid("Unsupported operationStatus.");
        String previous = state.getOperationStatus();
        state.changeOperation(status, command.reason().trim(), actor.userId());
        state = operationRepository.saveAndFlush(state);
        audit.record(actor, "STORE_OPERATION_STATUS_CHANGED", "STORE", id,
                Map.of("operationStatus", previous),
                Map.of("operationStatus", status, "version", state.getVersion()),
                null, command.reason(), request);
        return map(store, state);
    }

    @Transactional
    public Response visibility(Long id, StatusRequest command, AdminActor actor, HttpServletRequest request) {
        Restaurant store = findStore(id);
        AdminStoreOperation state = stateForUpdate(store, command.version());
        String status = normalize(command.status());
        if (!VISIBILITIES.contains(status)) throw invalid("Unsupported visibilityStatus.");
        String previous = state.getVisibilityStatus();
        state.changeVisibility(status, command.reason().trim(), actor.userId());
        state = operationRepository.saveAndFlush(state);

        store.update(store.getTitle(), store.getAddress(), store.getPhone(), store.getBusinessHours(),
                store.getIntroduction(), "visible".equals(status) ? "published" : "hidden");
        store = restaurantRepository.saveAndFlush(store);

        audit.record(actor, "STORE_VISIBILITY_CHANGED", "STORE", id,
                Map.of("visibilityStatus", previous),
                Map.of("visibilityStatus", status, "version", state.getVersion()),
                null, command.reason(), request);
        return map(store, state);
    }

    private Restaurant findStore(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND));
    }

    private AdminStoreOperation stateForUpdate(Restaurant store, Long expectedVersion) {
        AdminStoreOperation state = operationRepository.findById(store.getId())
                .orElseGet(() -> AdminStoreOperation.initial(store.getId(), defaultVisibility(store)));
        if (!Objects.equals(expectedVersion, state.getVersion())) {
            throw new AppException(ErrorCode.COMMON_CONFLICT);
        }
        return state;
    }

    private Response map(Restaurant store) {
        AdminStoreOperation state = operationRepository.findById(store.getId())
                .orElseGet(() -> AdminStoreOperation.initial(store.getId(), defaultVisibility(store)));
        return map(store, state);
    }

    private Response map(Restaurant store, AdminStoreOperation state) {
        return new Response(store.getId(), store.getTitle(), store.getAddress(), store.getPhone(),
                store.getBusinessHours(), store.getIntroduction(), state.getOperationStatus(),
                state.getVisibilityStatus(), state.getReason(), state.getUpdatedBy(), state.getUpdatedAt(),
                store.getCreatedAt(), store.getUpdatedAt(), state.getVersion());
    }

    private String defaultVisibility(Restaurant store) {
        return Set.of("published", "visible").contains(normalize(store.getExposureStatus())) ? "visible" : "hidden";
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw invalid("Invalid page or size.");
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        String result = trim(value);
        return result == null ? null : result.toLowerCase(Locale.ROOT);
    }

    private AppException invalid(String message) {
        return new AppException(ErrorCode.COMMON_INVALID_INPUT, message);
    }
}
