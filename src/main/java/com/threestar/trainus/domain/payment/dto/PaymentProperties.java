package com.threestar.trainus.domain.payment.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

	private String secretKey;
	private String baseUrl;
	private String confirmEndPoint;
	private String cancelEndPoint;
}
