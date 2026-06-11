package com.threestar.trainus.global.resolver;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.threestar.trainus.global.annotation.LoginUser;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@DisplayName("LoginUserArgumentResolver 단위 테스트")
class LoginUserArgumentResolverUnitTest {

	private final LoginUserArgumentResolver resolver = new LoginUserArgumentResolver();

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("@LoginUser가 붙은 Long 파라미터만 지원한다")
	void supportsParameter_WithLoginUserLong_ReturnsTrue() throws Exception {
		MethodParameter parameter = methodParameter("loginUserLong");

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test
	@DisplayName("@LoginUser가 없으면 Long 파라미터도 지원하지 않는다")
	void supportsParameter_WithoutLoginUser_ReturnsFalse() throws Exception {
		MethodParameter parameter = methodParameter("plainLong");

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test
	@DisplayName("@LoginUser가 있어도 Long 타입이 아니면 지원하지 않는다")
	void supportsParameter_WithLoginUserNonLong_ReturnsFalse() throws Exception {
		MethodParameter stringParameter = methodParameter("loginUserString");
		MethodParameter primitiveLongParameter = methodParameter("loginUserPrimitiveLong");

		assertThat(resolver.supportsParameter(stringParameter)).isFalse();
		assertThat(resolver.supportsParameter(primitiveLongParameter)).isFalse();
	}

	@Test
	@DisplayName("인증 정보가 없으면 AUTHENTICATION_REQUIRED 예외를 던진다")
	void resolveArgument_WhenAuthenticationIsNull_ThrowsAuthenticationRequired() {
		assertAuthenticationRequiredThrownBy(() -> resolver.resolveArgument(null, null, null, null));
	}

	@Test
	@DisplayName("인증되지 않은 사용자이면 AUTHENTICATION_REQUIRED 예외를 던진다")
	void resolveArgument_WhenAuthenticationIsUnauthenticated_ThrowsAuthenticationRequired() {
		Authentication authentication = mock(Authentication.class);
		given(authentication.isAuthenticated()).willReturn(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertAuthenticationRequiredThrownBy(() -> resolver.resolveArgument(null, null, null, null));
	}

	@Test
	@DisplayName("principal이 Long 타입이 아니면 AUTHENTICATION_REQUIRED 예외를 던진다")
	void resolveArgument_WhenPrincipalIsNotLong_ThrowsAuthenticationRequired() {
		Authentication authentication = mock(Authentication.class);
		given(authentication.isAuthenticated()).willReturn(true);
		given(authentication.getPrincipal()).willReturn("1");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertAuthenticationRequiredThrownBy(() -> resolver.resolveArgument(null, null, null, null));
	}

	@Test
	@DisplayName("인증되어도 principal이 null이면 AUTHENTICATION_REQUIRED 예외를 던진다")
	void resolveArgument_WhenPrincipalIsNull_ThrowsAuthenticationRequired() {
		Authentication authentication = mock(Authentication.class);
		given(authentication.isAuthenticated()).willReturn(true);
		given(authentication.getPrincipal()).willReturn(null);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertAuthenticationRequiredThrownBy(() -> resolver.resolveArgument(null, null, null, null));
	}

	@Test
	@DisplayName("principal이 Long이면 사용자 ID를 반환한다")
	void resolveArgument_WhenPrincipalIsLong_ReturnsUserId() throws Exception {
		Long userId = 1L;
		Authentication authentication = mock(Authentication.class);
		given(authentication.isAuthenticated()).willReturn(true);
		given(authentication.getPrincipal()).willReturn(userId);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Object result = resolver.resolveArgument(null, null, null, null);

		assertThat(result).isEqualTo(userId);
	}

	private void assertAuthenticationRequiredThrownBy(ThrowingCallable callable) {
		assertThatThrownBy(callable)
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED);
	}

	private MethodParameter methodParameter(String methodName) throws NoSuchMethodException {
		Method method = ParameterFixture.class.getDeclaredMethod(methodName, parameterType(methodName));
		return new MethodParameter(method, 0);
	}

	private Class<?> parameterType(String methodName) {
		return switch (methodName) {
			case "loginUserString" -> String.class;
			case "loginUserPrimitiveLong" -> long.class;
			default -> Long.class;
		};
	}

	@SuppressWarnings("unused")
	private static class ParameterFixture {

		void loginUserLong(@LoginUser Long userId) {
		}

		void plainLong(Long userId) {
		}

		void loginUserString(@LoginUser String userId) {
		}

		void loginUserPrimitiveLong(@LoginUser long userId) {
		}
	}
}
