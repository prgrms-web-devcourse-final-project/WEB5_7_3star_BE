package com.threestar.trainus.global.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.user.repository.CouponRepository;
import com.threestar.trainus.domain.coupon.user.repository.UserCouponRepository;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
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
	private final CouponRepository couponRepository;
	private final UserCouponRepository userCouponRepository;
	private final LessonParticipantRepository lessonParticipantRepository;

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

		// 5. 쿠폰 생성 (관리자가 생성)
		List<Coupon> coupons = createCoupons();

		// 6. 유저쿠폰 생성 (사용자들이 쿠폰 발급받은 데이터)
		createUserCoupons(students, coupons);

		// 7. 레슨 참여자 생성
		createLessonParticipants(students, lessons);

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
			profile.updateProfileImage("https://example.com/instructor" + (i + 1) + ".jpg");
			profile.updateProfileIntro(instructorNames[i] + "입니다. 최고의 레슨을 제공합니다!");
			profileRepository.save(profile);

			// ProfileMetadata 초기값으로 생성 (스케줄러 테스트용)
			ProfileMetadata metadata = ProfileMetadata.builder()
				.user(savedInstructor)
				.reviewCount(0)
				.rating(0.0)
				.build();
			profileMetadataRepository.save(metadata);

			instructors.add(savedInstructor);
		}

		// 관리자 계정 추가
		User admin = User.builder()
			.email("admin@test.com")
			.password(passwordEncoder.encode("admin123"))
			.nickname("관리자")
			.role(UserRole.ADMIN)  //관리자
			.build();

		User savedAdmin = userRepository.save(admin);

		// 관리자 Profile 생성
		Profile adminProfile = ProfileMapper.toDefaultEntity(savedAdmin);
		adminProfile.updateProfileImage("https://example.com/admin.jpg");
		profileRepository.save(adminProfile);

		// 관리자 ProfileMetadata 생성
		ProfileMetadata adminMetadata = ProfileMetadataMapper.toDefaultEntity(savedAdmin);
		profileMetadataRepository.save(adminMetadata);

		instructors.add(savedAdmin);

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
			profile.updateProfileImage("https://example.com/student" + (i + 1) + ".jpg");
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
			// 해당 강사의 레슨들 찾기
			List<Lesson> instructorLessons = lessons.stream()
				.filter(lesson -> lesson.getLessonLeader().equals(instructor.getId()))
				.toList();

			if (instructorLessons.isEmpty()) {
				continue;
			}

			// 실제 리뷰 생성 (랜덤하게 5-15개)
			int reviewCount = random.nextInt(11) + 5; // 5-15개

			for (int i = 0; i < reviewCount; i++) {
				User randomStudent = students.get(random.nextInt(students.size()));
				Lesson randomLesson = instructorLessons.get(random.nextInt(instructorLessons.size()));

				// 평점은 3.0-5.0 사이에서 랜덤 생성
				double reviewRating = 3.0 + random.nextDouble() * 2.0; // 3.0-5.0
				reviewRating = Math.round(reviewRating * 10) / 10.0; // 소수점 1자리

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

	//쿠폰 생성 메서드
	private List<Coupon> createCoupons() {
		List<Coupon> coupons = new ArrayList<>();

		// 다양한 쿠폰 생성
		String[] couponNames = {
			"신규 회원 환영 쿠폰",
			"여름 시즌 할인 쿠폰",
			"헬스 전용 할인권",
			"요가 클래스 특가 쿠폰",
			"VIP 회원 전용 쿠폰",
			"주말 특가 쿠폰",
			"첫 레슨 체험 쿠폰",
			"단체 할인 쿠폰",
			"생일 축하 쿠폰",
			"연말 감사 쿠폰"
		};

		CouponCategory[] categories = CouponCategory.values();
		CouponStatus[] statuses = {CouponStatus.ACTIVE, CouponStatus.ACTIVE, CouponStatus.INACTIVE}; // 대부분 활성화

		for (int i = 0; i < couponNames.length; i++) {
			LocalDateTime now = LocalDateTime.now();

			// 발급 기간 설정 (현재부터 30일간)
			LocalDateTime openAt = now.minusDays(random.nextInt(10)); // 이미 시작된 쿠폰들도 있게
			LocalDateTime closeAt = openAt.plusDays(30 + random.nextInt(30)); // 30-60일간

			// 사용 기한 설정 (발급 마감일로부터 추가 30일)
			LocalDateTime expirationDate = closeAt.plusDays(30);

			String discountPriceStr;
			if (random.nextBoolean()) {
				// 50% 확률로 고정 금액 할인 (1000-10000원)
				int discountAmount = (random.nextInt(10) + 1) * 1000;
				discountPriceStr = discountAmount + "원";
			} else {
				// 50% 확률로 비율 할인 (5%-50%)
				int discountPercent = (random.nextInt(10) + 1) * 5; // 5%, 10%, 15%, ..., 50%
				discountPriceStr = discountPercent + "%";
			}

			Coupon coupon = Coupon.builder()
				.name(couponNames[i])
				.expirationDate(expirationDate)
				.discountPrice(discountPriceStr) // 1000-10000원
				.minOrderPrice(generateMinOrderPrice()) // 10000-50000원
				.status(statuses[random.nextInt(statuses.length)])
				.quantity(generateCouponQuantity()) // 50-500개
				.category(categories[random.nextInt(categories.length)])
				.openAt(openAt)
				.closeAt(closeAt)
				.build();

			coupons.add(couponRepository.save(coupon));
		}

		log.info("쿠폰 {}개 생성 완료", coupons.size());
		return coupons;
	}

	// 유저쿠폰 생성 메서드 (사용자들이 쿠폰을 발급받은 데이터)
	private void createUserCoupons(List<User> students, List<Coupon> coupons) {
		List<UserCoupon> userCoupons = new ArrayList<>();

		// 활성화된 쿠폰들만 필터링
		List<Coupon> activeCoupons = coupons.stream()
			.filter(coupon -> coupon.getStatus() == CouponStatus.ACTIVE)
			.toList();

		if (activeCoupons.isEmpty()) {
			log.info("활성화된 쿠폰이 없어서 유저쿠폰을 생성하지 않습니다.");
			return;
		}

		// 각 학생이 몇 개의 쿠폰을 가질지 결정 (0-5개)
		for (User student : students) {
			int couponCount = random.nextInt(6); // 0-5개

			if (couponCount == 0) {
				continue; // 쿠폰이 없는 사용자도 있게
			}

			// 중복 방지를 위한 Set
			Set<Long> issuedCouponIds = new HashSet<>();

			for (int i = 0; i < couponCount; i++) {
				Coupon randomCoupon = activeCoupons.get(random.nextInt(activeCoupons.size()));

				// 이미 발급받은 쿠폰은 제외
				if (issuedCouponIds.contains(randomCoupon.getId())) {
					continue;
				}

				issuedCouponIds.add(randomCoupon.getId());

				// 쿠폰 만료일 설정 (원본 쿠폰의 만료일과 동일)
				LocalDateTime expirationDate = randomCoupon.getExpirationDate();

				// UserCoupon 생성 (기본적으로 ACTIVE 상태로 생성됨)
				UserCoupon userCoupon = new UserCoupon(student, randomCoupon, expirationDate);

				// 20% 확률로 이미 사용된 쿠폰으로 설정
				if (random.nextInt(100) < 20) {
					userCoupon.use(); // 상태를 INACTIVE로 변경하고 useDate 설정
				}

				userCoupons.add(userCoupon);
			}
		}

		userCouponRepository.saveAll(userCoupons);
		log.info("유저쿠폰 {}개 생성 완료", userCoupons.size());
	}

	// lessonParticipant 테이블에 데이터 삽입
	private void createLessonParticipants(List<User> students, List<Lesson> lessons) {
		List<LessonParticipant> participants = new ArrayList<>();
		Random rand = new Random();
		for (User student : students) {
			// 각 학생당 1~4개의 레슨 참여
			int joinCount = rand.nextInt(4) + 1;

			Set<Long> joinedLessonIds = new HashSet<>();

			for (int i = 0; i < joinCount; i++) {
				Lesson lesson = lessons.get(rand.nextInt(lessons.size()));

				// 중복 참여 방지
				if (joinedLessonIds.contains(lesson.getId()))
					continue;
				joinedLessonIds.add(lesson.getId());

				LessonParticipant participant = LessonParticipant.builder()
					.lesson(lesson)
					.user(student)
					.build();

				participants.add(participant);
			}
		}

		lessonParticipantRepository.saveAll(participants);
		log.info("레슨 참여자 {}명 생성 완료", participants.size());
	}

	// 최소 주문 금액 생성 (10000-50000원, 5000원 단위)
	private Integer generateMinOrderPrice() {
		return (random.nextInt(9) + 2) * 5000; // 10000-50000원
	}

	// 쿠폰 수량 생성 (50-500개, 50개 단위)
	private Integer generateCouponQuantity() {
		return (random.nextInt(10) + 1) * 50; // 50-500개
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

	private double generateRating() {
		// 3.0 ~ 5.0 사이의 평점 생성 (소수점 1자리)
		double rating = 3.0d + random.nextDouble() * 2.0d;
		return Math.round(rating * 10) / 10.0d;
	}
}
