package com.threestar.trainus.global.config;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JtsConfig {

	@Bean
	public GeometryFactory geometryFactory() {
		// 4326 : GPS 시스템 표준 좌표계
		return new GeometryFactory(new PrecisionModel(), 4326);
	}
}
