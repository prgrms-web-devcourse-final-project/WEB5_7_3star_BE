package com.threestar.trainus.domain.user.mapper;

import com.threestar.trainus.domain.user.dto.LoginResponseDto;
import com.threestar.trainus.domain.user.dto.SignupRequestDto;
import com.threestar.trainus.domain.user.dto.SignupResponseDto;
import com.threestar.trainus.domain.user.dto.UserInfoResponseDto;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMapper {

	public static User toEntity(SignupRequestDto request, String encodedPassword) {
		return User.builder()
			.email(request.email())
			.password(encodedPassword)
			.nickname(request.nickname())
			.role(UserRole.USER)
			.build();
	}

	public static SignupResponseDto toSignupResponseDto(User user) {
		return new SignupResponseDto(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			user.getRole(),
			user.getCreatedAt()
		);
	}

	public static LoginResponseDto toLoginResponseDto(User user) {
		return new LoginResponseDto(
			user.getId(),
			user.getEmail(),
			user.getNickname()
		);
	}

	public static UserInfoResponseDto toUserInfoResponseDto(User user) {
		return new UserInfoResponseDto(
			user.getId(),
			user.getNickname()
		);
	}
}
