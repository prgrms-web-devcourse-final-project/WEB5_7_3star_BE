package com.threestar.trainus.domain.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	@Query("""
		   	SELECT r 
		   	FROM Review r
		   	JOIN FETCH r.lesson l
		   	JOIN FETCH r.reviewer u
		   	LEFT JOIN FETCH u.profile p
          	LEFT JOIN FETCH u.profileMetadata pm
		   	WHERE r.reviewee.id = :userId
		""")
	List<Review> findByReviewee_Id(@Param("userId") Long userId);

	boolean existsByReviewer_IdAndLessonId(Long reviewerId, Long lessonId);

	@Query(value = """
		SELECT COUNT(*) FROM (
		    SELECT r.review_id 
		    FROM reviews r 
		    JOIN user u1 ON r.reviewee_id = u1.id AND u1.deleted_at IS NULL
		    JOIN user u2 ON r.reviewer_id = u2.id AND u2.deleted_at IS NULL
		    WHERE r.reviewee_id = :userId 
		    LIMIT :limit
		) t
		""", nativeQuery = true)
	Integer count(
		@Param("userId") Long userId,
		@Param("limit") int limit
	);

	@Query("SELECT COUNT(r) FROM Review r WHERE r.reviewee.id = :revieweeId")
	Integer countByRevieweeId(@Param("revieweeId") Long revieweeId);

	@Query("SELECT ROUND(AVG(r.rating), 2) FROM Review r WHERE r.reviewee.id = :revieweeId")
	Double findAverageRatingByRevieweeId(@Param("revieweeId") Long revieweeId);
}
