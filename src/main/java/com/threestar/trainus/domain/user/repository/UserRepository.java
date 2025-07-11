package com.threestar.trainus.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);

	Optional<User> findByEmail(String email);

	// 닉네임으로 사용자 찾기
	Optional<User> findByNickname(String nickname);

	// 삭제되지 않은 사용자만 조회
	Optional<User> findByIdAndDeletedAtIsNull(Long id);
}
