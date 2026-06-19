package com.plateapp.plate_main.auth.exception;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AccountConflictException extends AppException {

    private final Map<String, String> fieldErrors;

    public AccountConflictException(Map<String, String> fieldErrors) {
        super(ErrorCode.ACCOUNT_CONFLICT);
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
