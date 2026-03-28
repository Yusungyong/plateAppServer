package com.plateapp.plate_main.faq.service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.faq.dto.FaqListResponse;
import com.plateapp.plate_main.faq.dto.FaqResponse;
import com.plateapp.plate_main.faq.dto.FaqUpsertRequest;
import com.plateapp.plate_main.faq.entity.Fp900Faq;
import com.plateapp.plate_main.faq.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    @Transactional(readOnly = true)
    public FaqListResponse listFaqs(String category, String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        PageRequest pageable = PageRequest.of(
            safePage,
            safeSize,
            Sort.by(
                Sort.Order.desc("isPinned"),
                Sort.Order.asc("displayOrder"),
                Sort.Order.desc("faqId")
            )
        );

        Page<Fp900Faq> result = faqRepository.findPublishedFaqs(
            normalize(category),
            normalize(keyword),
            pageable
        );

        return new FaqListResponse(
            result.getContent().stream().map(this::toResponse).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext()
        );
    }

    @Transactional
    public FaqResponse getFaq(Integer faqId) {
        int updated = faqRepository.incrementViewCount(faqId);
        if (updated == 0) {
            throw new AppException(ErrorCode.COMMON_NOT_FOUND, "FAQ not found");
        }

        Fp900Faq faq = faqRepository.findPublishedByFaqId(faqId);
        if (faq == null) {
            throw new AppException(ErrorCode.COMMON_NOT_FOUND, "FAQ not found");
        }
        return toResponse(faq);
    }

    @Transactional
    public FaqResponse createFaq(String username, FaqUpsertRequest request) {
        Fp900Faq faq = Fp900Faq.create(
            username,
            normalize(request.category()),
            request.title().trim(),
            request.answer().trim(),
            request.isPinned(),
            defaultDisplayOrder(request.displayOrder()),
            normalizeStatusCode(request.statusCode())
        );
        return toResponse(faqRepository.save(faq));
    }

    @Transactional
    public FaqResponse updateFaq(Integer faqId, FaqUpsertRequest request) {
        Fp900Faq faq = faqRepository.findById(faqId)
            .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND, "FAQ not found"));

        faq.update(
            normalize(request.category()),
            request.title().trim(),
            request.answer().trim(),
            request.isPinned(),
            defaultDisplayOrder(request.displayOrder()),
            normalizeStatusCode(request.statusCode())
        );
        return toResponse(faqRepository.save(faq));
    }

    @Transactional
    public void deleteFaq(Integer faqId) {
        Fp900Faq faq = faqRepository.findById(faqId)
            .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND, "FAQ not found"));
        faqRepository.delete(faq);
    }

    private FaqResponse toResponse(Fp900Faq faq) {
        return new FaqResponse(
            faq.getFaqId(),
            faq.getCategory(),
            faq.getTitle(),
            faq.getAnswer(),
            faq.getUsername(),
            Boolean.TRUE.equals(faq.getIsPinned()),
            faq.getViewCount() == null ? 0 : faq.getViewCount(),
            faq.getDisplayOrder(),
            faq.getStatusCode(),
            faq.getCreatedAt(),
            faq.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatusCode(String value) {
        String trimmed = normalize(value);
        if (trimmed == null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "statusCode is required");
        }
        return trimmed;
    }

    private Integer defaultDisplayOrder(Integer displayOrder) {
        return displayOrder == null ? 0 : displayOrder;
    }
}
