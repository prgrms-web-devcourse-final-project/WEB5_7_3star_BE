package com.threestar.trainus.domain.metadata.repositroy;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;

public interface ProfileMetadataRepository extends JpaRepository<ProfileMetadata, Long> {

	Optional<ProfileMetadata> findByUserId(Long userId);
}
