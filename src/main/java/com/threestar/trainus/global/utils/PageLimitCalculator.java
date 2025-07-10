package com.threestar.trainus.global.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageLimitCalculator {

	public static int calculatePageLimit(int page, int pageSize, int movablePageCount) {
		return (((page - 1) / movablePageCount) + 1) * pageSize * movablePageCount + 1;
	}
}
