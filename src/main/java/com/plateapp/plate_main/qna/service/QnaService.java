package com.plateapp.plate_main.qna.service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.qna.dto.QnaCreateRequest;
import com.plateapp.plate_main.qna.dto.QnaListResponse;
import com.plateapp.plate_main.qna.dto.QnaResponse;
import com.plateapp.plate_main.qna.dto.QnaUpdateRequest;
import com.plateapp.plate_main.qna.entity.Fp901Qna;
import com.plateapp.plate_main.qna.repository.QnaRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QnaService {

    private static final Set<String> ALLOWED_STATUS_CODES =
        Set.of("received", "reviewing", "answered", "hidden");

    private final QnaRepository qnaRepository;

    @Transactional(readOnly = true)
    public QnaListResponse listQna(String category, String statusCode, int page, int size, boolean isAdmin) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        PageRequest pageable = PageRequest.of(
            safePage,
            safeSize,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("qnaId")
            )
        );

        Page<Fp901Qna> result;
        if (isAdmin) {
            result = qnaRepository.findAdminQna(
                normalize(category),
                normalizeStatusCodeOrNull(statusCode),
                pageable
            );
        } else {
            if (statusCode != null && !statusCode.isBlank()) {
                throw new AppException(ErrorCode.AUTH_FORBIDDEN, "statusCode filter is admin only");
            }
            result = qnaRepository.findPublicQna(normalize(category), pageable);
        }

        return new QnaListResponse(
            result.getContent().stream().map(qna -> toResponse(qna, isAdmin)).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public QnaResponse getQna(Integer qnaId, boolean isAdmin) {
        Fp901Qna qna = isAdmin
            ? qnaRepository.findById(qnaId).orElse(null)
            : qnaRepository.findPublicByQnaId(qnaId);

        if (qna == null) {
            throw new AppException(ErrorCode.COMMON_NOT_FOUND, "QnA not found");
        }
        return toResponse(qna, isAdmin);
    }

    @Transactional
    public QnaResponse createQna(String username, QnaCreateRequest request) {
        String guestName = normalize(request.guestName());
        String guestEmail = normalize(request.guestEmail());

        if (username == null && guestName == null && guestEmail == null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "guestName or guestEmail is required");
        }

        Fp901Qna qna = Fp901Qna.create(
            username,
            guestName,
            guestEmail,
            request.category().trim(),
            request.question().trim(),
            request.isPublic()
        );
        return toResponse(qnaRepository.save(qna), true);
    }

    @Transactional
    public QnaResponse updateQna(Integer qnaId, QnaUpdateRequest request) {
        Fp901Qna qna = qnaRepository.findById(qnaId)
            .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND, "QnA not found"));

        String statusCode = normalizeStatusCodeOrNull(request.statusCode());
        if (statusCode == null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "statusCode is required");
        }

        qna.answer(request.answer().trim(), statusCode, request.isPublic());
        return toResponse(qnaRepository.save(qna), true);
    }

    private QnaResponse toResponse(Fp901Qna qna, boolean isAdmin) {
        return new QnaResponse(
            qna.getQnaId(),
            qna.getUsername(),
            qna.getGuestName(),
            isAdmin ? qna.getGuestEmail() : null,
            qna.getCategory(),
            qna.getQuestion(),
            qna.getAnswer(),
            qna.getStatusCode(),
            Boolean.TRUE.equals(qna.getIsPublic()),
            qna.getCreatedAt(),
            qna.getUpdatedAt(),
            qna.getAnsweredAt()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatusCodeOrNull(String value) {
        String trimmed = normalize(value);
        if (trimmed == null) {
            return null;
        }
        if (!ALLOWED_STATUS_CODES.contains(trimmed)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Invalid statusCode");
        }
        return trimmed;
    }
}

