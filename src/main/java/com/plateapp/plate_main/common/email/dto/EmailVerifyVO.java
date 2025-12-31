// src/main/java/com/plateapp/platemain/common/email/dto/EmailVerifyVO.java
package com.plateapp.plate_main.common.email.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerifyVO {
    private String username;          // 선택적 (아이디 찾기/검증 시에 사용)
    private String email;
    private String verificationCode;
}
