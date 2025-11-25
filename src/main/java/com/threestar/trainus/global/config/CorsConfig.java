package com.threestar.trainus.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOrigins("http://localhost:3000",
				"http://localhost:8080",
				"http://localhost:8031",
				"http://15.165.184.145:8080",
				"http://15.165.184.145:3000",
				"http://15.165.184.145:8031")
			.allowCredentials(true) // 쿠키 허용
			.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
			.allowedHeaders("*");
	}
}
