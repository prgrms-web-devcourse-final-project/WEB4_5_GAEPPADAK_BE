package site.kkokkio.domain.admin.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/batch")
@Tag(name = "Admin V1", description = "관리자용 API 엔드포인트 V1")
public class BatchJobControllerV1 {

	private final JobLauncher jobLauncher;
	private final Job trendToPostJob;

	@Operation(summary = "스케줄러 실행")
	@PostMapping("/run/trend")
	public ResponseEntity<String> runTrendToPostJob() {
		try {
			// 요청 시점 (현재 시각)
			LocalDateTime bucketAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0);

			// JobParameters는 항상 고유해야 실행됨
			JobParameters params = new JobParametersBuilder()
				.addString("runTime", bucketAt.toString())
				.toJobParameters();

			jobLauncher.run(trendToPostJob, params);
			return ResponseEntity.ok("trendToPostJob 실행: " + bucketAt);

		} catch (Exception e) {
			log.error("배치 실행 중 예외 발생", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("실행 실패: " + e.getMessage());
		}
	}
}