package com.threestar.trainus.domain.lesson.teacher.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "location")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Location {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String city;      // 시도명
	private String district;  // 시군구명
	private String dong;      // 읍면동명
	private String ri;        // 리명

	public Location(String city, String district, String dong, String ri) {
		this.city = city;
		this.district = district;
		this.dong = dong;
		this.ri = ri;
	}
}