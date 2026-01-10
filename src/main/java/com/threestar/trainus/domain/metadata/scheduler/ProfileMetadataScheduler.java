package com.threestar.trainus.domain.metadata.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.metadata.service.ProfileMetadataService;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileMetadataScheduler {

	private final ProfileMetadataService profileMetadataService;
	private final UserRepository userRepository;

	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul") // 매일 자정 실행
	public void updateAllProfileMetadata() {
		try {
			List<User> instructors = userRepository.findByRole(UserRole.USER);

			for (User instructor : instructors) {
				try {
					profileMetadataService.batchUpdateMetadata(instructor.getId());
				} catch (Exception e) {
					log.warn("강사 ID {}의 메타데이터 업데이트 실패: {}", instructor.getId(), e.getMessage());
				}
			}
		} catch (Exception e) {
			log.error("프로필 메타데이터 배치 업데이트 중 오류 발생", e);
		}
	}
}