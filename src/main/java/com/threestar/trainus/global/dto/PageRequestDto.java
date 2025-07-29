package com.threestar.trainus.global.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter // @ModelAttribute 바인딩을 위한 setter, 외부노출X
@RequiredArgsConstructor
public class PageRequestDto {

	@Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
	@Max(value = 1000, message = "페이지는 1000 이하여야 합니다.")
	private int page = 1;

	@Min(value = 1, message = "limit는 1 이상이어야 합니다.")
	@Max(value = 100, message = "limit는 100 이하여야 합니다.")
	private int limit = 5;

}
