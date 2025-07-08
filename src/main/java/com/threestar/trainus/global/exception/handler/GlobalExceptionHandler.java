package com.threestar.trainus.global.exception.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.threestar.trainus.global.exception.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
		ErrorResponse errorResponse = ErrorResponse.builder()
			.status(exception.getErrorCode().getStatus().value())
			.code(exception.getErrorCode().name())
			.message(exception.getErrorCode().getMessage())
			.build();

		return ResponseEntity.status(exception.getErrorCode().getStatus()).body(errorResponse);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(err -> err.getField() + ": " + err.getDefaultMessage())
			.findFirst()
			.orElse("입력값이 유효하지 않습니다.");

		ErrorResponse errorResponse = ErrorResponse.builder()
			.status(400)
			.code("VALIDATION_FAILED")
			.message(message)
			.build();

		return ResponseEntity.badRequest().body(errorResponse);
	}
}
