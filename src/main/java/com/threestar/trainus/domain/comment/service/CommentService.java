package com.threestar.trainus.domain.comment.service;

import static java.util.function.Predicate.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.comment.dto.CommentCreateRequestDto;
import com.threestar.trainus.domain.comment.dto.CommentResponseDto;
import com.threestar.trainus.domain.comment.entity.Comment;
import com.threestar.trainus.domain.comment.mapper.CommentMapper;
import com.threestar.trainus.domain.comment.repository.CommentRepository;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final UserRepository userRepository;
	private final LessonRepository lessonRepository;
	private final CommentRepository commentRepository;

	@Transactional
	public CommentResponseDto createComment(CommentCreateRequestDto request, Long lessonId, Long userId) {
		Lesson findLesson = lessonRepository.findById(lessonId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
		User findUser = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		Comment parent = findParent(request);
		Comment newComment = commentRepository.save(
			Comment.builder()
				.lesson(findLesson)
				.user(findUser)
				.deleted(false)
				.content(request.getContent())
				.parentCommentId(parent == null ? null : parent.getCommentId())
				.build()
		);
		return CommentMapper.toCommentResponseDto(newComment);
	}

	private Comment findParent(CommentCreateRequestDto request) {
		Long parentCommentId = request.getParentCommentId();
		if (parentCommentId == null) {
			return null;
		}
		return commentRepository.findById(parentCommentId)
			.filter(not(Comment::getDeleted))
			.filter(Comment::isRoot)
			.orElseThrow();
	}

}
