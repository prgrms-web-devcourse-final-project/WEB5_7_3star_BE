package com.threestar.trainus.domain.lesson.admin.dto;

import lombok.Builder;

/**
 * 프로필 아직 없어서 임시로 만들었음!!!간단한 유저 정보
 */
@Builder
public record UserSimpleDto(
	Long id,
	String nickname,
	String profileImage
) {
}

// TODO: profile 도메인 완성 후 ProfileDto(아마도 이거 겠죠?) 사용, 이 클래스 삭제할예정



