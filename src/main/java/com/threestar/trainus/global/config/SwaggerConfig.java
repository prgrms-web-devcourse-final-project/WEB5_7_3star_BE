package com.threestar.trainus.global.config;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class SwaggerConfig {
	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
			.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
			.components(new Components()
				.addSecuritySchemes("bearerAuth", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
				)
			)
			.info(apiInfo())
			.tags(apiTags());
	}

	private Info apiInfo() {
		return new Info()
			.title("TrainUs API 문서") // API의 제목
			.description("운동 메이트 매칭 플랫폼의 API 명세서") // API에 대한 설명
			.version("1.0.0"); // API의 버전
	}

	@Bean
	public OpenApiCustomizer tagOrderCustomizer() {
		return openApi -> {
			if (openApi.getTags() == null) {
				return;
			}

			Map<String, Integer> tagOrder = IntStream.range(0, apiTags().size())
				.boxed()
				.collect(Collectors.toMap(i -> apiTags().get(i).getName(), i -> i));

			openApi.setTags(openApi.getTags().stream()
				.sorted(Comparator.comparingInt(tag -> tagOrder.getOrDefault(tag.getName(), Integer.MAX_VALUE)))
				.toList());
		};
	}

	private List<Tag> apiTags() {
		return List.of(
			new Tag().name("유저 API"),
			new Tag().name("수강생 레슨 API"),
			new Tag().name("강사용 레슨 API"),
			new Tag().name("댓글 API"),
			new Tag().name("리뷰 API"),
			new Tag().name("쿠폰 API"),
			new Tag().name("관리자 쿠폰 API"),
			new Tag().name("결제 API"),
			new Tag().name("랭킹 조회 API"),
			new Tag().name("유저 프로필 API"),
			new Tag().name("지역 API"),
			new Tag().name("시스템 API"),
			new Tag().name("파일 API"),
			new Tag().name("테스트 - S3 API"),
			new Tag().name("테스트 - 동시성 API")
		);
	}
}
