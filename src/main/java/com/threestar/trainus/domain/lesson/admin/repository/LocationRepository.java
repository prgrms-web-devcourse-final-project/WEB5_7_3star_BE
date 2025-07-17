package com.threestar.trainus.domain.lesson.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.lesson.admin.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
	boolean existsByCityAndDistrictAndDongAndRi(String city, String district, String dong, String ri);

	boolean existsByCityAndDistrictAndDong(String city, String district, String dong);
}