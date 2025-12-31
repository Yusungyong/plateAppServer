// src/main/java/com/plateapp/plate_main/auth/exception/AuthException.java
package com.plateapp.plate_main.auth.exception;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;

public class AuthException extends AppException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
