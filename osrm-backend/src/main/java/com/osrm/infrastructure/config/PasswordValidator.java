package com.osrm.infrastructure.config;

import com.osrm.common.exception.BizException;

/**
 * 密码策略校验器
 */
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;

    /**
     * 校验密码强度
     * 规则：至少8位，必须包含字母和数字
     */
    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BizException("密码长度至少" + MIN_LENGTH + "位");
        }

        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }

        if (!hasLetter || !hasDigit) {
            throw new BizException("密码必须包含字母和数字");
        }
    }
}
