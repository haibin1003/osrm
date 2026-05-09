package com.osrm.common.exception;

import com.osrm.common.model.ApiResponse;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException e) {
        Integer code = e.getCode() != null ? e.getCode() : 500;
        HttpStatus status;
        // 当 code 为默认 500（即 throw new BizException(message) 未显式指定 code）时，
        // 根据消息文案推断更精确的状态码（业务校验失败默认应为 400）
        if (code == 500) {
            status = inferStatusFromMessage(e.getMessage());
            if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
                status = HttpStatus.BAD_REQUEST;
            }
            return ResponseEntity.status(status).body(ApiResponse.error(status.value(), e.getMessage()));
        }
        status = mapCodeToHttpStatus(code);
        return ResponseEntity.status(status).body(ApiResponse.error(code, e.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "用户名或密码错误"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, e.getMessage() != null ? e.getMessage() : "认证失败"));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(JwtException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "令牌无效或已过期"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, "无权访问该资源"));
    }

    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, e.getMessage() != null ? e.getMessage() : "资源不存在"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("请求参数校验失败");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("请求参数错误");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst()
                .orElse("参数约束校验失败");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "缺少必需的参数: " + e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "参数类型错误: " + e.getName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(405, "不支持的请求方法: " + e.getMethod()));
    }

    /**
     * 兜底：识别已知的业务运行时异常文案，匹配出更精确的 HTTP 状态码。
     * 这是为了兼容历史代码中大量直接 throw new RuntimeException(...) 的场景。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        HttpStatus status = inferStatusFromMessage(msg);

        if (status.is5xxServerError()) {
            logger.error("运行时异常", e);
        } else {
            logger.warn("业务异常 [{}]: {}", status.value(), msg);
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), msg.isEmpty() ? "服务异常" : msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        logger.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "系统错误，请稍后重试"));
    }

    /**
     * 将 BizException.code 映射为 HTTP 状态码：
     * - 400~499：直接作为 HTTP 状态
     * - 500+：HTTP 500
     * - 其它：HTTP 200（业务返回，状态码体现在 body.code）
     */
    private HttpStatus mapCodeToHttpStatus(int code) {
        if (code >= 400 && code < 500) {
            HttpStatus s = HttpStatus.resolve(code);
            return s != null ? s : HttpStatus.BAD_REQUEST;
        }
        if (code >= 500) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }

    /**
     * 根据异常消息推断 HTTP 状态码。
     * 历史代码中 service 大量使用 new RuntimeException(中文消息)，这里做语义识别兜底。
     */
    private HttpStatus inferStatusFromMessage(String msg) {
        if (msg == null || msg.isEmpty()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // 401: 认证类
        if (msg.contains("用户名或密码")
                || msg.contains("账户已被禁用")
                || msg.contains("账户已被锁定")
                || msg.contains("令牌")
                || msg.contains("Token")
                || msg.contains("token")
                || msg.contains("未登录")
                || msg.contains("认证失败")) {
            return HttpStatus.UNAUTHORIZED;
        }
        // 404: 资源不存在
        if (msg.contains("不存在") || msg.contains("未找到") || msg.contains("不在")) {
            return HttpStatus.NOT_FOUND;
        }
        // 400: 业务校验失败（已存在/重复/必填/格式/状态错误等）
        if (msg.contains("已存在") || msg.contains("重复")
                || msg.contains("必填") || msg.contains("不能为空")
                || msg.contains("格式") || msg.contains("长度")
                || msg.contains("无效") || msg.contains("非法")
                || msg.contains("请先") || msg.contains("不能") || msg.contains("已")
                || msg.contains("至少") || msg.contains("超过")) {
            return HttpStatus.BAD_REQUEST;
        }
        // 默认：保留 500
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
