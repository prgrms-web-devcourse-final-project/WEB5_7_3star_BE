package com.threestar.trainus.domain.file.service;

import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.threestar.trainus.domain.file.dto.GetS3UrlDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class S3Service {
	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	private final AmazonS3 amazonS3;

	public GetS3UrlDto getPostS3Url(String prefix, String fileName) {
		if (!prefix.isEmpty()) {
			fileName = createPath(prefix, fileName);
		}

		GeneratePresignedUrlRequest generatePresignedUrlRequest = getGeneratePresignedUrlRequest(bucket, fileName);
		URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);

		return GetS3UrlDto.builder()
			.preSignedUrl(url.toString())
			.key(fileName)
			.build();
	}

	// 파일 업로드용 Presigned URL 생성
	private GeneratePresignedUrlRequest getGeneratePresignedUrlRequest(String bucket, String fileName) {
		return new GeneratePresignedUrlRequest(bucket, fileName)
			.withMethod(HttpMethod.PUT)
			.withExpiration(getPresignedUrlExpiration());
	}

	//Presigned URL 유효기간 설정
	private Date getPresignedUrlExpiration() {
		Date expiration = new Date();
		long expTimeMillis = expiration.getTime();
		expTimeMillis += 1000 * 60 * 10;
		expiration.setTime(expTimeMillis);

		return expiration;
	}

	//UUID를 사용하여 파일 고유 ID 생성
	private String createFileId() {
		return UUID.randomUUID().toString();
	}

	//파일의 전체 경로 생성
	private String createPath(String prefix, String fileName) {
		String fileId = createFileId();
		return String.format("%s/%s", prefix, fileId + "-" + fileName);
	}

	// get 용 URL 생성
	private GeneratePresignedUrlRequest getGetGeneratePresignedUrlRequest(String key, Date expiration) {
		return new GeneratePresignedUrlRequest(bucket, key)
			.withMethod(HttpMethod.GET)
			.withExpiration(expiration);
	}

	@Transactional(readOnly = true)
	public GetS3UrlDto getGetS3Url(String key) {
		// url 유효기간 설정하기(1시간)
		Date expiration = getExpiration();

		// presigned url 생성하기
		GeneratePresignedUrlRequest generatePresignedUrlRequest =
			getGetGeneratePresignedUrlRequest(key, expiration);

		URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);

		// return
		return GetS3UrlDto.builder()
			.preSignedUrl(url.toExternalForm())
			.key(key)
			.build();
	}

	private static Date getExpiration() {
		Date expiration = new Date();
		long expTimeMillis = expiration.getTime();
		expTimeMillis += 1000 * 60 * 10; // 10분으로 설정하기
		expiration.setTime(expTimeMillis);
		return expiration;
	}
}
