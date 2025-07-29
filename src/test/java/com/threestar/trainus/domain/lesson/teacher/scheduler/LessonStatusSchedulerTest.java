package com.threestar.trainus.domain.lesson.teacher.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;

@ExtendWith(MockitoExtension.class)
class LessonStatusSchedulerTest {
	@Mock
	private LessonRepository lessonRepository;

	@InjectMocks
	private LessonStatusScheduler lessonStatusScheduler;

	@Test
	@DisplayName("시작할 레슨들을 진행중으로 변경하는 스케줄러 테스트")
	void updateLessonStatus_StartLessons() {
		LocalDateTime now = LocalDateTime.now();

		Lesson lessonToStart = Lesson.builder()
			.lessonLeader(1L)
			.lessonName("테스트 레슨")
			.description("테스트 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(now.minusMinutes(30)) // 30분 전에 시작
			.endAt(now.plusHours(1))
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.addressDetail("테스트 주소")
			.build();

		given(lessonRepository.findLessonsToStart(any(LocalDateTime.class)))
			.willReturn(List.of(lessonToStart));
		given(lessonRepository.findLessonsToComplete(any(LocalDateTime.class)))
			.willReturn(List.of());

		lessonStatusScheduler.updateLessonStatus();

		verify(lessonRepository).findLessonsToStart(any(LocalDateTime.class));
		verify(lessonRepository).findLessonsToComplete(any(LocalDateTime.class));
		verify(lessonRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("완료할 레슨들을 완료 상태로 변경하는 스케줄러 테스트")
	void updateLessonStatus_CompleteLessons() {
		LocalDateTime now = LocalDateTime.now();

		Lesson lessonToComplete = Lesson.builder()
			.lessonLeader(1L)
			.lessonName("테스트 레슨")
			.description("테스트 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(now.minusHours(2)) // 2시간 전에 시작
			.endAt(now.minusMinutes(30)) // 30분 전에 종료
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.addressDetail("테스트 주소")
			.build();

		given(lessonRepository.findLessonsToStart(any(LocalDateTime.class)))
			.willReturn(List.of());
		given(lessonRepository.findLessonsToComplete(any(LocalDateTime.class)))
			.willReturn(List.of(lessonToComplete));

		lessonStatusScheduler.updateLessonStatus();

		verify(lessonRepository).findLessonsToStart(any(LocalDateTime.class));
		verify(lessonRepository).findLessonsToComplete(any(LocalDateTime.class));
		verify(lessonRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("처리할 레슨이 없는 경우 스케줄러 테스트")
	void updateLessonStatus_NoLessonsToProcess() {
		given(lessonRepository.findLessonsToStart(any(LocalDateTime.class)))
			.willReturn(List.of());
		given(lessonRepository.findLessonsToComplete(any(LocalDateTime.class)))
			.willReturn(List.of());

		lessonStatusScheduler.updateLessonStatus();

		verify(lessonRepository).findLessonsToStart(any(LocalDateTime.class));
		verify(lessonRepository).findLessonsToComplete(any(LocalDateTime.class));
		verify(lessonRepository, never()).saveAll(anyList());
	}

	@Test
	@DisplayName("시작과 완료를 동시에 처리하는 스케줄러 테스트")
	void updateLessonStatus_StartAndCompleteLessons() {
		LocalDateTime now = LocalDateTime.now();

		Lesson lessonToStart = Lesson.builder()
			.lessonLeader(1L)
			.lessonName("시작할 레슨")
			.description("테스트 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(now.minusMinutes(10)) // 10분 전에 시작
			.endAt(now.plusHours(1))
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.addressDetail("테스트 주소")
			.build();

		Lesson lessonToComplete = Lesson.builder()
			.lessonLeader(2L)
			.lessonName("완료할 레슨")
			.description("테스트 설명")
			.category(Category.YOGA)
			.price(25000)
			.maxParticipants(8)
			.startAt(now.minusHours(2))
			.endAt(now.minusMinutes(10)) // 10분 전에 종료
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.addressDetail("테스트 주소")
			.build();

		given(lessonRepository.findLessonsToStart(any(LocalDateTime.class)))
			.willReturn(List.of(lessonToStart));
		given(lessonRepository.findLessonsToComplete(any(LocalDateTime.class)))
			.willReturn(List.of(lessonToComplete));

		lessonStatusScheduler.updateLessonStatus();
		
		verify(lessonRepository).findLessonsToStart(any(LocalDateTime.class));
		verify(lessonRepository).findLessonsToComplete(any(LocalDateTime.class));
		verify(lessonRepository, times(2)).saveAll(anyList()); // 시작과 완료 각각 한 번씩
	}
}
