package site.kkokkio.global.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HourScheduler {
	private final JobLauncher jobLauncher;
	private final Job trendingKeywordsJob;

	@Scheduled(cron = "0 0 * * * *")
	public void runTrendingKeywordsJob() throws
		JobExecutionAlreadyRunningException,
		JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		LocalDateTime bucketAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
			.withMinute(0).withSecond(0).withNano(0); // bucketAt 매시 정각 설정
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("runTime", bucketAt.toString())
			.toJobParameters();
		jobLauncher.run(trendingKeywordsJob, jobParameters);
	}
}
