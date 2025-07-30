package com.threestar.trainus.domain.comment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.comment.dto.CommentWithUserProjection;
import com.threestar.trainus.domain.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	//create index idx_lesson_id_parent_comment_id_comment_id on comments(lesson_id, parent_comment_id asc, comment_id asc); 인덱스 통해 조회 성능 최적화
	@Query(value = """
			SELECT 
				c.comment_id AS commentId,
				c.content AS content,
				c.parent_comment_id AS parentCommentId,
				c.deleted AS deleted,
				c.created_at AS createdAt,
				u.id AS userId,
				u.nickname AS nickname
			FROM (
				SELECT comment_id
				FROM comments c
				JOIN user u ON c.user_id = u.id AND u.deleted_at IS NULL
				WHERE lesson_id = :lessonId
		  	    	ORDER BY parent_comment_id ASC, comment_id ASC
		  	    	LIMIT :limit OFFSET :offset
			) t
			JOIN comments c ON t.comment_id = c.comment_id
			JOIN user u ON c.user_id = u.id
		""", nativeQuery = true)
	List<CommentWithUserProjection> findAll(@Param("lessonId") Long lessonId, @Param("offset") int offset,
		@Param("limit") int limit);

	@Query(value = """
		SELECT count(*) FROM ( 
		    SELECT comment_id 
		    FROM comments c
		    JOIN user u ON c.user_id = u.id AND u.deleted_at IS NULL
		    WHERE lesson_id = :lessonId 
		    LIMIT :limit
		) t
		""", nativeQuery = true)
	Integer count(@Param("lessonId") Long lessonId, @Param("limit") int limit);

	@Query(value = """
		SELECT count(*) FROM (
		    SELECT comment_id 
		    FROM comments c
		    JOIN user u ON c.user_id = u.id AND u.deleted_at IS NULL
		    WHERE lesson_id = :lessonId AND parent_comment_id = :parentCommentId 
		    LIMIT :limit
		) t
		""", nativeQuery = true)
	Long countBy(@Param("lessonId") Long lessonId, @Param("parentCommentId") Long parentCommentId,
		@Param("limit") int limit);

	Optional<Comment> findByCommentIdAndUserId(Long commentId, Long userId);

}
