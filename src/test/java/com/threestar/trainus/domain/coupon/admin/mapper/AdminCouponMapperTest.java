package com.threestar.trainus.domain.coupon.admin.mapper;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponDetailResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponListResponseDto;
import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

class AdminCouponMapperTest {

	@Test
	@DisplayName("쿠폰 생성 요청을 쿠폰 엔티티로 매핑한다")
	void toEntity_MapsCreateRequest() {
		LocalDateTime now = LocalDateTime.now();
		CouponCreateRequestDto request = new CouponCreateRequestDto(
			"신규 쿠폰",
			now.plusDays(30),
			"5000원",
			10000,
			CouponStatus.ACTIVE,
			100,
			CouponCategory.NORMAL,
			now,
			now.plusDays(7)
		);

		Coupon coupon = AdminCouponMapper.toEntity(request);

		assertThat(coupon.getName()).isEqualTo("신규 쿠폰");
		assertThat(coupon.getExpirationDate()).isEqualTo(now.plusDays(30));
		assertThat(coupon.getDiscountPrice()).isEqualTo("5000원");
		assertThat(coupon.getMinOrderPrice()).isEqualTo(10000);
		assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
		assertThat(coupon.getQuantity()).isEqualTo(100);
		assertThat(coupon.getCategory()).isEqualTo(CouponCategory.NORMAL);
	}

	@Test
	@DisplayName("쿠폰 상세 응답은 발급 수와 쿠폰 필드를 매핑한다")
	void toCouponDetailResponseDto_MapsFields() {
		Coupon coupon = createCoupon(1L, "상세 쿠폰");

		CouponDetailResponseDto response = AdminCouponMapper.toCouponDetailResponseDto(coupon, 12);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.couponName()).isEqualTo("상세 쿠폰");
		assertThat(response.discountPrice()).isEqualTo("20%");
		assertThat(response.issuedCount()).isEqualTo(12);
		assertThat(response.couponCategory()).isEqualTo(CouponCategory.OPEN_RUN);
	}

	@Test
	@DisplayName("쿠폰 목록 응답은 totalCount와 쿠폰 item 목록을 매핑한다")
	void toCouponListResponseDto_MapsListAndCount() {
		Coupon first = createCoupon(1L, "첫 쿠폰");
		Coupon second = createCoupon(2L, "둘 쿠폰");

		CouponListResponseDto response = AdminCouponMapper.toCouponListResponseDto(List.of(first, second), 10);

		assertThat(response.totalCount()).isEqualTo(10);
		assertThat(response.couponList()).extracting("couponId").containsExactly(1L, 2L);
		assertThat(AdminCouponMapper.toCouponListWrapperDto(response).coupons()).hasSize(2);
	}

	private Coupon createCoupon(Long couponId, String name) {
		LocalDateTime now = LocalDateTime.now();
		Coupon coupon = Coupon.builder()
			.name(name)
			.expirationDate(now.plusDays(30))
			.discountPrice("20%")
			.minOrderPrice(10000)
			.status(CouponStatus.ACTIVE)
			.quantity(50)
			.category(CouponCategory.OPEN_RUN)
			.openAt(now.minusDays(1))
			.closeAt(now.plusDays(7))
			.build();
		ReflectionTestUtils.setField(coupon, "id", couponId);
		return coupon;
	}
}
