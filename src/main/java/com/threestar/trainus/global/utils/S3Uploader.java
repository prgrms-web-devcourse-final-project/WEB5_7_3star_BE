package com.threestar.trainus.global.utils;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class S3Uploader {

	private final AmazonS3 s3Client;

	@Value("${cloud.aws.s3.bucket}")
	private String bucketName;

	public String uploadFile(MultipartFile file, String folder) throws IOException {
		// 고유한 파일 이름 생성
		String uniqueFileName = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

		// 메타데이터 설정
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.getSize());
		metadata.setContentType(file.getContentType());

		// S3에 파일 업로드
		s3Client.putObject(new PutObjectRequest(bucketName, uniqueFileName, file.getInputStream(), metadata));

		// 업로드된 파일의 URL 반환
		return s3Client.getUrl(bucketName, uniqueFileName).toString();
	}
}
