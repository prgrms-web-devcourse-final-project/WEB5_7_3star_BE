package com.threestar.trainus.global.unit;

import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    private final int status;
    private String message;
    private T data;

    public BaseResponse(int status) {
        this.status = status;
    }

    public BaseResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    //메세지 주고 싶은경우 -> 초기 프론트 연결용
    //성공 메시지와 데이터 반환 (사용하지 않으면 null 넣어주기)
    public static <T> ResponseEntity<BaseResponse<T>> ok(String message, T data, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(new BaseResponse<>(status.value(), message, data));
    }

    //성공 시 아무것도 반환하고 싶지 않을 때 사용
    public static ResponseEntity<BaseResponse<Void>> okOnlyStatus(HttpStatus status) {
        return ResponseEntity.status(status)
                .body(new BaseResponse<>(status.value()));
    }

    //에러 메시지와 데이터를 반환 (사용하지 않으면 null 넣어주기)
    public static <T> ResponseEntity<BaseResponse<T>> error(String message, T data, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(new BaseResponse<>(status.value(), message, data));
    }

    //에러 발생 시 아무것도 반환하고 싶지 않을 때 사용
    public static ResponseEntity<BaseResponse<Void>> errorOnlyStatus(HttpStatus status) {
        return ResponseEntity.status(status)
                .body(new BaseResponse<>(status.value()));
    }
}
