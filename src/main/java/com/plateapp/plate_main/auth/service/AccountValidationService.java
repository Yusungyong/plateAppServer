package com.plateapp.plate_main.auth.service;

import com.plateapp.plate_main.auth.exception.AccountConflictException;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountValidationService {

    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_NICKNAME = "nickname";

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9]{4,30}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int EMAIL_MAX_LENGTH = 320;
    private static final int NICKNAME_MAX_LENGTH = 100;

    private static final Map<String, String> AVAILABLE_MESSAGES = Map.of(
            FIELD_USERNAME, "사용 가능한 회원 ID입니다.",
            FIELD_EMAIL, "사용 가능한 이메일입니다.",
            FIELD_NICKNAME, "사용 가능한 닉네임입니다."
    );
    private static final Map<String, String> DUPLICATE_MESSAGES = Map.of(
            FIELD_USERNAME, "이미 사용 중인 회원 ID입니다.",
            FIELD_EMAIL, "이미 가입된 이메일입니다.",
            FIELD_NICKNAME, "이미 사용 중인 닉네임입니다."
    );

    private final UserRepository userRepository;

    public ValidationResult validateAvailability(String rawField, String rawValue) {
        String field = normalizeField(rawField);
        String value = normalizeAndValidate(field, rawValue);
        boolean available = !exists(field, value);
        return new ValidationResult(
                field,
                value,
                available,
                available ? AVAILABLE_MESSAGES.get(field) : DUPLICATE_MESSAGES.get(field)
        );
    }

    public NormalizedAccount normalizeAccount(String username, String email, String nickname) {
        return new NormalizedAccount(
                normalizeAndValidate(FIELD_USERNAME, username),
                normalizeAndValidate(FIELD_EMAIL, email),
                normalizeAndValidate(FIELD_NICKNAME, nickname)
        );
    }

    public void assertAvailable(NormalizedAccount account) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (userRepository.existsById(account.username())) {
            fieldErrors.put(FIELD_USERNAME, DUPLICATE_MESSAGES.get(FIELD_USERNAME));
        }
        if (userRepository.existsByEmailIgnoreCase(account.email())) {
            fieldErrors.put(FIELD_EMAIL, DUPLICATE_MESSAGES.get(FIELD_EMAIL));
        }
        if (userRepository.existsByNickname(account.nickname())) {
            fieldErrors.put(FIELD_NICKNAME, DUPLICATE_MESSAGES.get(FIELD_NICKNAME));
        }
        if (!fieldErrors.isEmpty()) {
            throw new AccountConflictException(fieldErrors);
        }
    }

    public AccountConflictException conflictFrom(DataIntegrityViolationException exception) {
        String detail = rootMessage(exception).toLowerCase(Locale.ROOT);
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (detail.contains("uq_fp_100_email_normalized") || detail.contains("(email)")) {
            fieldErrors.put(FIELD_EMAIL, DUPLICATE_MESSAGES.get(FIELD_EMAIL));
        } else if (detail.contains("uq_fp_100_nickname_normalized") || detail.contains("(nick_name)")) {
            fieldErrors.put(FIELD_NICKNAME, DUPLICATE_MESSAGES.get(FIELD_NICKNAME));
        } else if (detail.contains("fp_100_pkey") || detail.contains("(username)")) {
            fieldErrors.put(FIELD_USERNAME, DUPLICATE_MESSAGES.get(FIELD_USERNAME));
        } else {
            fieldErrors.put("account", "이미 사용 중인 계정 정보입니다.");
        }
        return new AccountConflictException(fieldErrors);
    }

    private boolean exists(String field, String value) {
        return switch (field) {
            case FIELD_USERNAME -> userRepository.existsById(value);
            case FIELD_EMAIL -> userRepository.existsByEmailIgnoreCase(value);
            case FIELD_NICKNAME -> userRepository.existsByNickname(value);
            default -> throw invalid("지원하지 않는 계정 필드입니다.");
        };
    }

    private String normalizeField(String rawField) {
        String field = rawField == null ? "" : rawField.trim().toLowerCase(Locale.ROOT);
        if (!FIELD_USERNAME.equals(field) && !FIELD_EMAIL.equals(field) && !FIELD_NICKNAME.equals(field)) {
            throw invalid("field는 username, email, nickname 중 하나여야 합니다.");
        }
        return field;
    }

    private String normalizeAndValidate(String field, String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        switch (field) {
            case FIELD_USERNAME -> {
                if (!USERNAME_PATTERN.matcher(value).matches()) {
                    throw invalid("회원 ID는 영문과 숫자로 구성된 4~30자여야 합니다.");
                }
            }
            case FIELD_EMAIL -> {
                value = value.toLowerCase(Locale.ROOT);
                if (value.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(value).matches()) {
                    throw invalid("이메일 형식이 올바르지 않습니다.");
                }
            }
            case FIELD_NICKNAME -> {
                if (value.isBlank() || value.length() > NICKNAME_MAX_LENGTH) {
                    throw invalid("닉네임은 1~100자여야 합니다.");
                }
            }
            default -> throw invalid("지원하지 않는 계정 필드입니다.");
        }
        return value;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }

    private AppException invalid(String message) {
        return new AppException(ErrorCode.COMMON_INVALID_INPUT, message);
    }

    public record ValidationResult(String field, String value, boolean available, String message) {
    }

    public record NormalizedAccount(String username, String email, String nickname) {
    }
}
