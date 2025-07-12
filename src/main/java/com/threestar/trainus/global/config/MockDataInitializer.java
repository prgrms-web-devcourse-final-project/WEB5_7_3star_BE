package com.threestar.trainus.global.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.admin.entity.Category;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.repository.LessonRepository;
import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;
import com.threestar.trainus.domain.metadata.mapper.ProfileMetadataMapper;
import com.threestar.trainus.domain.metadata.repository.ProfileMetadataRepository;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.profile.mapper.ProfileMapper;
import com.threestar.trainus.domain.profile.repository.ProfileRepository;
import com.threestar.trainus.domain.review.entity.Review;
import com.threestar.trainus.domain.review.repository.ReviewRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@org.springframework.context.annotation.Profile("dev") // dev 프로필에서만 실행
@RequiredArgsConstructor
public class MockDataInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final ProfileMetadataRepository profileMetadataRepository;
	private final LessonRepository lessonRepository;
	private final ReviewRepository reviewRepository;
	private final PasswordEncoder passwordEncoder;

	private final Random random = new Random();

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		if (userRepository.count() > 0) {
			log.info("데이터가 이미 존재합니다. Mock 데이터 생성을 건너뜁니다.");
			return;
		}

		log.info("Mock 데이터 생성 시작...");

		// 1. 강사 유저 생성 (15명)
		List<User> instructors = createInstructors();

		// 2. 수강생 유저 생성 (50명)
		List<User> students = createStudents();

		// 3. 각 강사별로 레슨 생성
		List<Lesson> lessons = createLessons(instructors);

		// 4. 리뷰 생성 (ProfileMetadata 업데이트)
		createReviews(instructors, students, lessons);

		log.info("Mock 데이터 생성 완료!");
		log.info("생성된 데이터: 강사 {}명, 수강생 {}명, 레슨 {}개",
			instructors.size(), students.size(), lessons.size());
	}

	private List<User> createInstructors() {
		List<User> instructors = new ArrayList<>();
		String[] instructorNames = {
			"헬스왕김철수", "요가마스터", "필라테스여신", "러닝코치박", "수영선수이",
			"테니스프로", "복싱챔피언", "클라이밍킹", "골프레슨프로", "댄스퀸",
			"크로스핏코치", "배드민턴고수", "탁구선수", "농구코치", "축구감독"
		};

		for (int i = 0; i < instructorNames.length; i++) {
			User instructor = User.builder()
				.email("instructor" + (i + 1) + "@test.com")
				.password(passwordEncoder.encode("password123"))
				.nickname(instructorNames[i])
				.role(UserRole.USER)
				.build();

			User savedInstructor = userRepository.save(instructor);

			// Profile 생성
			Profile profile = ProfileMapper.toDefaultEntity(savedInstructor);
			profile.updateProfile(
				"https://example.com/instructor" + (i + 1) + ".jpg",
				instructorNames[i] + "입니다. 최고의 레슨을 제공합니다!"
			);
			profileRepository.save(profile);

			// ProfileMetadata 생성 (랭킹용 데이터)
			ProfileMetadata metadata = ProfileMetadata.builder()
				.user(savedInstructor)
				.reviewCount(generateReviewCount())
				.rating(generateRating())
				.build();
			profileMetadataRepository.save(metadata);

			instructors.add(savedInstructor);
		}

		return instructors;
	}

	private List<User> createStudents() {
		List<User> students = new ArrayList<>();

		for (int i = 0; i < 50; i++) {
			User student = User.builder()
				.email("student" + (i + 1) + "@test.com")
				.password(passwordEncoder.encode("password123"))
				.nickname("수강생" + (i + 1))
				.role(UserRole.USER)
				.build();

			User savedStudent = userRepository.save(student);

			// Profile 생성
			Profile profile = ProfileMapper.toDefaultEntity(savedStudent);
			profile.updateProfile(
				"https://example.com/student" + (i + 1) + ".jpg",
				"운동을 열심히 하는 수강생입니다!"
			);
			profileRepository.save(profile);

			// 수강생은 메타데이터 기본값
			ProfileMetadata metadata = ProfileMetadataMapper.toDefaultEntity(savedStudent);
			profileMetadataRepository.save(metadata);

			students.add(savedStudent);
		}

		return students;
	}

	private List<Lesson> createLessons(List<User> instructors) {
		List<Lesson> lessons = new ArrayList<>();
		Category[] categories = Category.values();

		for (User instructor : instructors) {
			// 각 강사당 2-4개의 레슨 생성
			int lessonCount = random.nextInt(3) + 2;

			for (int i = 0; i < lessonCount; i++) {
				Category category = categories[random.nextInt(categories.length)];

				Lesson lesson = Lesson.builder()
					.lessonLeader(instructor.getId())
					.lessonName(category.name() + " 레슨" + (i + 1))
					.description("최고의 " + category.name() + " 레슨입니다!")
					.maxParticipants(random.nextInt(10) + 5) // 5-14명
					.startAt(LocalDateTime.now().plusDays(random.nextInt(30) + 1))
					.endAt(LocalDateTime.now().plusDays(random.nextInt(30) + 1).plusHours(2))
					.price(random.nextInt(50000) + 10000) // 10,000-60,000원
					.category(category)
					.openTime(null)
					.openRun(false)
					.city("서울시")
					.district("강남구")
					.dong("역삼동")
					.addressDetail("테스트 주소 " + random.nextInt(100))
					.build();

				lessons.add(lessonRepository.save(lesson));
			}
		}

		return lessons;
	}

	private void createReviews(List<User> instructors, List<User> students, List<Lesson> lessons) {
		for (User instructor : instructors) {
			ProfileMetadata metadata = profileMetadataRepository.findByUserId(instructor.getId())
				.orElseThrow();

			// 해당 강사의 레슨들 찾기
			List<Lesson> instructorLessons = lessons.stream()
				.filter(lesson -> lesson.getLessonLeader().equals(instructor.getId()))
				.toList();

			if (instructorLessons.isEmpty()) continue;

			// reviewCount만큼 실제 리뷰 생성
			int reviewCount = metadata.getReviewCount();

			for (int i = 0; i < reviewCount; i++) {
				User randomStudent = students.get(random.nextInt(students.size()));
				Lesson randomLesson = instructorLessons.get(random.nextInt(instructorLessons.size()));

				// 평점은 metadata의 rating 주변으로 생성
				float baseRating = metadata.getRating();
				float reviewRating = Math.max(1.0f, Math.min(5.0f,
					baseRating + (random.nextFloat() - 0.5f) * 2)); // ±1점 범위

				Review review = Review.builder()
					.reviewer(randomStudent)
					.reviewee(instructor)
					.lesson(randomLesson)
					.rating(reviewRating)
					.content("좋은 레슨이었습니다! 추천해요.")
					.image(random.nextBoolean() ? "https://example.com/review" + i + ".jpg" : null)
					.build();

				reviewRepository.save(review);
			}
		}
	}

	private int generateReviewCount() {
		// 랭킹에 들어갈 강사들(20개 이상)과 그렇지 않은 강사들 섞어서 생성
		int rand = random.nextInt(100);
		if (rand < 60) { // 60% 확률로 랭킹 대상
			return random.nextInt(50) + 20; // 20-69개
		} else { // 40% 확률로 랭킹 비대상
			return random.nextInt(20); // 0-19개
		}
	}

	private float generateRating() {
		// 3.0 ~ 5.0 사이의 평점 생성 (소수점 1자리)
		float rating = 3.0f + random.nextFloat() * 2.0f;
		return Math.round(rating * 10) / 10.0f;
	}
}