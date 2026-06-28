package com.campuslink.common;

import com.campuslink.entity.OperationLog;
import com.campuslink.mapper.OperationLogMapper;
import com.campuslink.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，统一转换为 {@link Result}。
 *
 * <p>v2 起对未捕获的系统异常做持久化（写 {@code operation_log}，status=FAIL），
 * 弥补 {@link com.campuslink.aspect.OperationLogAspect} 仅记录写操作、不覆盖
 * GET 请求异常的盲区，满足「异常日志持久化」需求。
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final OperationLogMapper operationLogMapper;

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.error(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);
        persistException(e, request);
        return Result.error(ResultCode.ERROR, e.getMessage());
    }

    /**
     * 将未捕获异常持久化为 FAIL 日志，失败不影响主流程。
     */
    private void persistException(Exception e, HttpServletRequest request) {
        try {
            OperationLog log = new OperationLog();
            if (request != null) {
                log.setMethod(request.getMethod());
                log.setPath(request.getRequestURI());
                log.setIp(request.getRemoteAddr());
            } else {
                log.setMethod("-");
                log.setPath("-");
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
                log.setUserId(loginUser.getUserId());
                log.setUsername(loginUser.getUsername());
            }
            log.setStatus("FAIL");
            log.setCostMs(0);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.setErrorMsg(msg.length() > 480 ? msg.substring(0, 480) : msg);
            operationLogMapper.insert(log);
        } catch (Exception ignore) {
            GlobalExceptionHandler.log.warn("异常日志持久化失败: {}", ignore.getMessage());
        }
    }
}
