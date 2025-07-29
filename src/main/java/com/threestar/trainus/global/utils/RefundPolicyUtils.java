package com.threestar.trainus.global.utils;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.payment.entity.Payment;

public class RefundPolicyUtils {
	public static int getRefundPrice(LocalDateTime cancelTime, Payment payment) {
		int refundPrice = 0;
		if (cancelTime.isBefore(payment.getLesson().getStartAt().minusMonths(1))) {
			refundPrice = payment.getPayPrice();
		} else if (cancelTime.isBefore(payment.getLesson().getStartAt().minusWeeks(1))) {
			refundPrice = (payment.getPayPrice() * 50 / 100);
		} else if (cancelTime.isBefore(payment.getLesson().getStartAt().minusDays(3))) {
			refundPrice = (payment.getPayPrice() * 30 / 100);
		} else {
			refundPrice = (payment.getPayPrice() * 30 / 100);
		}
		return refundPrice;
	}
}
