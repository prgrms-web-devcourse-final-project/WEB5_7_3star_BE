package com.threestar.trainus.domain.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
	List<Review> findByReviewee_Id(Long userId);

	@Query(
		value = "select count(*) from ("
			+ " select review_id from reviews where reviewee_id = :userId limit :limit"
			+ ") t",
		nativeQuery = true
	)
	Long count(
		@Param("userId") Long userId,
		@Param("limit") int limit
	);
}
