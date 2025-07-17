package com.threestar.trainus.domain.lesson.admin.service;

import static com.threestar.trainus.global.exception.domain.ErrorCode.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.threestar.trainus.domain.lesson.admin.entity.Location;
import com.threestar.trainus.domain.lesson.admin.repository.LocationRepository;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationCsvService {

	private final LocationRepository locationRepository;

	public void processCsv(MultipartFile file) {
		List<Location> locations = new ArrayList<>();

		try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
			CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

			String[] row;
			while ((row = csvReader.readNext()) != null) {
				String city = row[1].trim();
				String district = row[2].trim();
				String dong = row[3].trim();
				String ri = row[4].trim();

				// 동이 존재하는 경우에 저장
				if (!city.isBlank() && !district.isBlank() && !dong.isBlank()) {
					locations.add(new Location(city, district, dong, ri));
				}
			}

			locationRepository.saveAll(locations);

		} catch (IOException | CsvException e) {
			throw new BusinessException(INTERNAL_SERVER_ERROR);
		}
	}

	public boolean checkLocation(String city, String district, String dong, String ri) {
		if (ri == null || ri.isBlank()) {
			return locationRepository.existsByCityAndDistrictAndDong(city, district, dong);
		} else {
			return locationRepository.existsByCityAndDistrictAndDongAndRi(city, district, dong, ri);
		}
	}
}
