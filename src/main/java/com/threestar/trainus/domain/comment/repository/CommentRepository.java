package com.threestar.trainus.domain.comment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@Query(
		value = "select comments.comment_id, comments.lesson_id, comments.user_id, comments.content,"
			+ " comments.parent_comment_id, comments.deleted, comments.created_at, comments.updated_at "
			+ " from ("
			+ "	select comment_id from comments where lesson_id = :lessonId "
			+ " order by parent_comment_id asc, comment_id asc "
			+ " limit :limit offset :offset "
			+ ") t left join comments on t.comment_id = comments.comment_id",
		nativeQuery = true
	)
	List<Comment> findAll(
		@Param("lessonId") Long lessonId,
		@Param("offset") Long offset,
		@Param("limit") Long limit
	);

	@Query(
		value = "select count(*) from ("
		+ " select comment_id from comments where lesson_id = :lessonId limit :limit"
		+ ") t",
		nativeQuery = true
	)
	Long count(
		@Param("lessonId") Long lessonId,
		@Param("limit") Long limit
	);
}
