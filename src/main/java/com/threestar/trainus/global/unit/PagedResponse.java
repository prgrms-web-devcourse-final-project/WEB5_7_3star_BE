package com.threestar.trainus.global.unit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {
	private final int status;
	private final String message;
	private final T data;
	private final int count;

	public static <T> ResponseEntity<PagedResponse<T>> ok(String message, T data, int count, HttpStatus status) {
		return ResponseEntity.status(status)
			.body(PagedResponse.<T>builder()
				.status(status.value())
				.message(message)
				.data(data)
				.count(count)
				.build());
	}
}
