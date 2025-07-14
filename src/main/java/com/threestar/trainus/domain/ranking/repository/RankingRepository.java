package com.threestar.trainus.domain.ranking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
    WHERE pm.reviewCount >= 20
    ORDER BY (
        (pm.rating / 5.0) * 0.5 + 
        (LEAST(pm.reviewCount, 100) / 100.0) * 0.5
    ) DESC
    LIMIT 10
    """)
	List<RankingData> findTopRankings();

}