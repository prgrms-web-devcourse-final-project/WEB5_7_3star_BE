package com.threestar.trainus;

import java.util.TimeZone;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TrainUsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrainUsApplication.class, args);
	}

	@Bean
	public CommandLineRunner init() {
		return args -> {
			TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		};
	}
}
