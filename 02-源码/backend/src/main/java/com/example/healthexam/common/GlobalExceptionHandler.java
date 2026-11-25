package com.example.healthexam.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** 全局异常处理：所有异常统一转成 R 结构，不向前端暴露堆栈 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValid(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException m && m.getBindingResult().getFieldError() != null) {
            msg = m.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof BindException b && b.getBindingResult().getFieldError() != null) {
            msg = b.getBindingResult().getFieldError().getDefaultMessage();
        }
        return R.fail(400, msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleUpload(MaxUploadSizeExceededException e) {
        return R.fail(400, "文件超过 20MB 限制");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return R.fail(500, "系统繁忙：" + e.getMessage());
    }
}
