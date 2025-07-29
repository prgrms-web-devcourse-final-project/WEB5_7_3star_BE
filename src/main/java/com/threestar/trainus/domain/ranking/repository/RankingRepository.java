package com.threestar.trainus.domain.ranking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;
import com.threestar.trainus.domain.ranking.dto.RankingData;

public interface RankingRepository extends JpaRepository<ProfileMetadata, Long> {

	@Query("""
		SELECT pm.user.id as userId,
			pm.user.nickname as userNickname,
			pm.reviewCount as reviewCount,
			pm.rating as rating,
			p.profileImage as profileImage
		FROM ProfileMetadata pm
		JOIN pm.user u
		LEFT JOIN Profile p ON p.user = u
		WHERE pm.reviewCount >= 5
		ORDER BY (
			(pm.rating / 5.0) * 0.5 +
			(LEAST(pm.reviewCount, 100) / 100.0) * 0.5
		) DESC
		LIMIT 10
		""")
	List<RankingData> findTopRankings();

	@Query("""
		SELECT r.reviewee.id as userId,
			r.reviewee.nickname as userNickname,
			CAST(COUNT(r.reviewId) AS INTEGER) as reviewCount,
			CAST(AVG(r.rating) AS DOUBLE) as rating,
			p.profileImage as profileImage
		FROM Review r
		JOIN r.lesson l
		JOIN r.reviewee u
		LEFT JOIN Profile p ON p.user = u
		WHERE l.category = :category
		AND r.deletedAt IS NULL
		GROUP BY r.reviewee.id, r.reviewee.nickname, p.profileImage
		HAVING COUNT(r.reviewId) >= 5
		ORDER BY (
			(AVG(r.rating) / 5.0) * 0.5 +
			(LEAST(COUNT(r.reviewId), 100) / 100.0) * 0.5
		) DESC
		LIMIT 10
		""")
	List<RankingData> findTopRankingsByCategory(@Param("category") Category category);
}
