package com.threestar.trainus.domain.test.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.profile.service.ProfileFacadeService;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestUserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ProfileFacadeService profileFacadeService;

	@Transactional
	public User findOrCreateUser(Long userId) {
		Optional<User> existingUser = userRepository.findById(userId);
		if (existingUser.isPresent()) {
			return existingUser.get();
		}
		String email = "testuser" + userId + "@example.com";
		String nickname = "testuser" + userId;

		if (userRepository.existsByEmail(email) || userRepository.existsByNickname(nickname)) {
			return userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("테스트 유저 생성 실패"));
		}

		String encodedPassword = passwordEncoder.encode("password");
		User newUser = User.builder()
			.email(email)
			.password(encodedPassword)
			.nickname(nickname)
			.role(UserRole.USER)
			.build();

		User savedUser = userRepository.save(newUser);
		profileFacadeService.createDefaultProfile(savedUser);

		return savedUser;
	}

	@Transactional
	public User findOrCreateUser2(Long userId) {

		return userRepository.findByTestUserId(userId)
			.orElseGet(() -> {
				User user = User.builder()
					.testUserId(userId)
					.email("testuser" + userId + "@example.com")
					.nickname("testuser" + userId)
					.password(passwordEncoder.encode("password"))
					.role(UserRole.USER)
					.build();

				User saved = userRepository.save(user);
				profileFacadeService.createDefaultProfile(saved);
				return saved;
			});
	}

}
