package com.threestar.trainus.domain.file.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class GetS3UrlDto {
	private String preSignedUrl;
	private String key;

	@Builder
	public GetS3UrlDto(String preSignedUrl, String key) {
		this.preSignedUrl = preSignedUrl;
		this.key = key;
	}
}
