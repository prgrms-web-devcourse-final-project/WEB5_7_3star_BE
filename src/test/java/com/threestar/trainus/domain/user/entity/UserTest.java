package com.threestar.trainus.domain.user.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;

class UserTest {

	@Test
	@DisplayName("사용자 탈퇴 시 deletedAt이 설정된다")
	void withdraw_shouldSetDeletedAt() {
		// given
		User user = User.builder()
			.email("test@example.com")
			.password("encodedPassword")
			.nickname("testUser")
			.role(UserRole.USER)
			.build();

		// when
		LocalDateTime beforeWithdraw = LocalDateTime.now();
		user.withdraw();
		LocalDateTime afterWithdraw = LocalDateTime.now();

		// then
		assertThat(user.getDeletedAt()).isNotNull();
		assertThat(user.getDeletedAt()).isBetween(beforeWithdraw, afterWithdraw);
	}

	@Test
	@DisplayName("비밀번호 업데이트가 정상적으로 동작한다")
	void updatePassword_shouldUpdatePassword() {
		// given
		User user = User.builder()
			.email("test@example.com")
			.password("oldPassword")
			.nickname("testUser")
			.role(UserRole.USER)
			.build();

		String newPassword = "newEncodedPassword";

		// when
		user.updatePassword(newPassword);

		// then
		assertThat(user.getPassword()).isEqualTo(newPassword);
	}

	@Test
	@DisplayName("사용자가 ADMIN 역할을 가질 수 있다")
	void user_canHaveAdminRole() {
		// given & when
		User adminUser = User.builder()
			.email("admin@example.com")
			.password("adminPassword")
			.nickname("admin")
			.role(UserRole.ADMIN)
			.build();

		// then
		assertThat(adminUser.getRole()).isEqualTo(UserRole.ADMIN);
	}
}