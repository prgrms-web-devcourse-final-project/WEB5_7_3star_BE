// package com.threestar.trainus.coupon.user;
//
// import static org.mockito.BDDMockito.*;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
// import java.time.LocalDateTime;
// import java.util.List;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.context.annotation.ComponentScan;
// import org.springframework.context.annotation.FilterType;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
// import org.springframework.test.web.servlet.MockMvc;
//
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.threestar.trainus.domain.coupon.user.controller.CouponController;
// import com.threestar.trainus.domain.coupon.user.dto.CouponPageResponseDto;
// import com.threestar.trainus.domain.coupon.user.dto.CouponResponseDto;
// import com.threestar.trainus.domain.coupon.user.dto.CreateUserCouponResponseDto;
// import com.threestar.trainus.domain.coupon.user.dto.UserCouponPageResponseDto;
// import com.threestar.trainus.domain.coupon.user.dto.UserCouponResponseDto;
// import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
// import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
// import com.threestar.trainus.domain.coupon.user.entity.OwnedStatus;
// import com.threestar.trainus.domain.coupon.user.service.CouponService;
// import com.threestar.trainus.global.config.security.SessionAuthenticationFilter;
// import com.threestar.trainus.global.resolver.LoginUserArgumentResolver;
//
// @WebMvcTest(controllers = CouponController.class, excludeFilters = {
// 	@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SessionAuthenticationFilter.class)
// })
// @AutoConfigureMockMvc(addFilters = false) //시큐리티 인증 생략
// public class UserCouponControllerTests {
//
// 	@Autowired
// 	private MockMvc mockMvc;
//
// 	@Autowired
// 	private ObjectMapper objectMapper;
//
// 	@MockitoBean
// 	private CouponService couponService;
//
// 	@MockitoBean
// 	private LoginUserArgumentResolver loginUserArgumentResolver;
//
// 	private static final Long userId = 1L;
//
// 	@BeforeEach
// 	void setUp() throws Exception {
// 		given(loginUserArgumentResolver.supportsParameter((any())))
// 			.willReturn(true);
// 		given(loginUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
// 			.willReturn(userId);
// 	}
//
// 	@Test
// 	@DisplayName("쿠폰 발급 API 테스트")
// 	void createUserCoupon() throws Exception {
// 		Long couponId = 100L;
// 		CreateUserCouponResponseDto responseDto = new CreateUserCouponResponseDto(
// 			couponId,
// 			userId,
// 			LocalDateTime.now(),
// 			LocalDateTime.now().plusDays(30),
// 			CouponStatus.ACTIVE
// 		);
//
// 		given(couponService.createUserCoupon(userId, couponId))
// 			.willReturn(responseDto);
//
// 		mockMvc.perform(post("/api/v1/coupons/{couponId}", couponId)
// 				.with(csrf()))
// 			.andExpect(status().isCreated())
// 			.andExpect(jsonPath("$.message").value("쿠폰 발급 완료"))
// 			.andExpect(jsonPath("$.data.couponId").value(couponId))
// 			.andExpect(jsonPath("$.data.status").value(CouponStatus.ACTIVE.toString()));
// 	}
//
// 	@Test
// 	@DisplayName("내 쿠폰 목록 조회 API 테스트")
// 	void getUserCoupons() throws Exception {
// 		List<UserCouponResponseDto> userCouponList = List.of(
// 			new UserCouponResponseDto(
// 				100L,
// 				"신규 회원 할인 쿠폰",
// 				"5000",
// 				30000,
// 				LocalDateTime.now().plusDays(30),
// 				CouponStatus.ACTIVE,
// 				null
// 			),
// 			new UserCouponResponseDto(
// 				101L,
// 				"VIP 회원 할인 쿠폰",
// 				"10000",
// 				50000,
// 				LocalDateTime.now().plusDays(15),
// 				CouponStatus.ACTIVE,
// 				LocalDateTime.now().minusDays(1)
// 			),
// 			new UserCouponResponseDto(
// 				102L,
// 				"기간 만료 쿠폰",
// 				"3000",
// 				20000,
// 				LocalDateTime.now().minusDays(5),
// 				CouponStatus.INACTIVE,
// 				null
// 			)
// 		);
// 		UserCouponPageResponseDto dto = new UserCouponPageResponseDto(userCouponList);
//
// 		given(couponService.getUserCoupons(userId, null))
// 			.willReturn(dto);
//
// 		mockMvc.perform(get("/api/v1/coupons/my-coupons"))
// 			.andExpect(status().isOk())
// 			.andExpect(jsonPath("$.message").value("사용자 보유 쿠폰 조회 성공"))
// 			.andExpect(jsonPath("$.data.userCoupons[0].couponId").value(100L))
// 			.andExpect(jsonPath("$.data.userCoupons[1].couponId").value(101L))
// 			.andExpect(jsonPath("$.data.userCoupons[2].couponId").value(102L));
//
// 	}
//
// 	@Test
// 	@DisplayName("발급 가능 쿠폰 목록 조회 API 테스트")
// 	void getCoupons() throws Exception {
// 		Long couponId = 100L;
//
// 		CouponPageResponseDto dto = new CouponPageResponseDto(
// 			List.of(
// 				new CouponResponseDto(
// 					100L,
// 					"신규 회원 할인 쿠폰",
// 					"5000",
// 					30000,
// 					LocalDateTime.now().plusDays(30),
// 					OwnedStatus.NOT_OWNED,
// 					100,
// 					CouponCategory.OPEN_RUN,
// 					LocalDateTime.now()
// 				),
// 				new CouponResponseDto(
// 					101L,
// 					"VIP 회원 할인 쿠폰",
// 					"10000",
// 					50000,
// 					LocalDateTime.now().plusDays(10),
// 					OwnedStatus.OWNED,
// 					50,
// 					CouponCategory.OPEN_RUN,
// 					LocalDateTime.now().minusDays(1)
// 				)
// 			)
// 		);
//
// 		given(couponService.getCoupons(userId))
// 			.willReturn(dto);
//
// 		mockMvc.perform(get("/api/v1/coupons"))
// 			.andExpect(status().isOk())
// 			.andExpect(jsonPath("$.message").value("발급가능한 쿠폰 조회 성공"))
// 			.andExpect(jsonPath("$.data.coupons[0].couponId").value(couponId));
//
// 	}
// }
