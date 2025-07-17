package com.threestar.trainus.domain.payment.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.payment.entity.PaymentMethod;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PaymentResponseDto {
	private String orderId;
	private String lessonTitle;
	private Integer originPrice;
	private Integer payPrice;
	private PaymentMethod paymentMethod;
	private LocalDateTime expiredAt;
}
