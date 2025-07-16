package com.threestar.trainus.domain.payment.config;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import lombok.extern.slf4j.Slf4j;

// @Slf4j
// public class PaymentLoggingInterceptor implements ClientHttpRequestInterceptor {
//
// 	@Override
// 	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws
// 		IOException {
// 		String host = request.getURI().getHost();
// 		String path = request.getURI().getPath();
// 		String httpMethod = request.getMethod().toString();
// 		log.info("[Payment request] {}\t: {} {} \n \t{}", path, httpMethod, host, new String(body));
// 		return execution.execute(request, body);
// 	}
// }
