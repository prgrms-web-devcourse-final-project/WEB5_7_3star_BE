package com.threestar.trainus.domain.payment.entity;

public enum PaymentMethod {
	CREDIT_CARD,
	BANK_TRANSFER,
	TOSS_PAYMENT;

	public static PaymentMethod fromTossMethod(String tossMethod) {
		return switch (tossMethod) {
			case "카드" -> CREDIT_CARD;
			case "계좌이체" -> BANK_TRANSFER;
			case "간편결제" -> TOSS_PAYMENT;
			default -> throw new IllegalArgumentException("지원하지 않는 결제 수단: " + tossMethod);
		};
	}
}
