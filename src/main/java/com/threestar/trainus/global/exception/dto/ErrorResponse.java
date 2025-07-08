package com.threestar.trainus.global.exception.dto;


import lombok.Builder;

@Builder
public record ErrorResponse(int status, String code, String message) {

}
