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
			select 
				c.comment_id as commentId,
				c.content as content,
				c.parent_comment_id as parentCommentId,
				c.deleted as deleted,
				c.created_at as createdAt,
				u.id as userId,
				u.nickname as nickname
			from (
				select comment_id
				from comments c
				join users u on c.user_id = u.id and u.deleted_at IS NULL
				where lesson_id = :lessonId
		  	    	order by parent_comment_id asc, comment_id asc
		  	    	limit :limit offset :offset
			) t
			join comments c on t.comment_id = c.comment_id
			join users u on c.user_id = u.id
		""", nativeQuery = true)
	List<CommentWithUserProjection> findAll(@Param("lessonId") Long lessonId, @Param("offset") int offset,
		@Param("limit") int limit);

	@Query(value = """
		select count(*) from ( 
		    select comment_id 
		    from comments c
		    join users u on c.user_id = u.id and u.deleted_at IS NULL
		    where lesson_id = :lessonId 
		    limit :limit
		) t
		""", nativeQuery = true)
	Integer count(@Param("lessonId") Long lessonId, @Param("limit") int limit);

	@Query(value = """
		select count(*) from (
		    select comment_id 
		    from comments c
		    join users u on c.user_id = u.id and u.deleted_at IS NULL
		    where lesson_id = :lessonId and parent_comment_id = :parentCommentId 
		    limit :limit
		) t
		""", nativeQuery = true)
	Long countBy(@Param("lessonId") Long lessonId, @Param("parentCommentId") Long parentCommentId,
		@Param("limit") int limit);

	Optional<Comment> findByCommentIdAndUserId(Long commentId, Long userId);

}
