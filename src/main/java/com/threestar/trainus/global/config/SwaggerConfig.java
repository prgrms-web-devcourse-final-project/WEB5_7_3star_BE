package com.threestar.trainus.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;

@Configuration
public class SwaggerConfig {
	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.addSecurityItem(new SecurityRequirement().addList("session"))
			.components(new Components()
				.addSecuritySchemes("session", new SecurityScheme()
					.type(SecurityScheme.Type.APIKEY)
					.in(SecurityScheme.In.COOKIE)
					.name("JSESSIONID")
				)
			)
			.info(apiInfo());
	}

	private Info apiInfo() {
		return new Info()
			.title("FitMate API 문서") // API의 제목
			.description("운동 메이트 매칭 플랫폼의 API 명세서") // API에 대한 설명
			.version("1.0.0"); // API의 버전
	}
}