package com.threestar.trainus.domain.metadata.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;

import jakarta.persistence.LockModeType;

public interface ProfileMetadataRepository extends JpaRepository<ProfileMetadata, Long> {

	Optional<ProfileMetadata> findByUserId(Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from ProfileMetadata p where p.user.id = :userId")
	Optional<ProfileMetadata> findWithLockByUserId(@Param("userId") Long userId);

}
