package com.backstage.curtaincall.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class CustomExHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CustomExResponse> handleCustomEx(CustomException e) {
        return CustomExResponse.toResponse(e.getCustomErrorCode());
    }

    // @Valid 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomExResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {


        BindingResult bindingResult = ex.getBindingResult();
        String errorMessage = bindingResult.getFieldError() != null
                ? bindingResult.getFieldError().getDefaultMessage()
                : "유효성 검증 실패";
        log.error(" @Valid 검증 실패 처리: {}", errorMessage);
        return CustomExResponse.toResponse(CustomErrorCode.VALIDATION_ERROR, errorMessage);
    }

    // NullPointerException 처리
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<CustomExResponse> handleNullPointerException(NullPointerException e) {
        log.error("NullPointerException", e);
        return CustomExResponse.toResponse(CustomErrorCode.NPE);
    }

    // 그 외 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomExResponse> handleException(Exception e) {
        log.error("Exception", e);
        return CustomExResponse.toResponse(CustomErrorCode.INTERNAL_SERVER_ERROR);
    }
}
