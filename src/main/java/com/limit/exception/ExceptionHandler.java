package com.limit.exception;


import com.limit.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(value = Exception.class)
    public ResponseVo handleException(Exception e) {
        log.error("系统异常", e);
        return new ResponseVo().setStatus(HttpStatus.INTERNAL_SERVER_ERROR).setMessage("系统异常");
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(value = LimitException.class)
    public ResponseVo handleLimitAccessException(LimitException e) {
        log.debug("出现限流异常LimitException：", e);
        return new ResponseVo().setStatus(HttpStatus.TOO_MANY_REQUESTS).setMessage(e.getMessage());
    }

}
