package com.threestar.trainus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class TrainUsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrainUsApplication.class, args);
	}

}
