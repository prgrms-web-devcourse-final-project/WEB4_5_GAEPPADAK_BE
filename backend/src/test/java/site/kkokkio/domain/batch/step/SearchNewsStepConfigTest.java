package site.kkokkio.domain.batch.step;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import site.kkokkio.domain.batch.context.BatchConstants;
import site.kkokkio.domain.batch.context.ExecutionContextKeys;
import site.kkokkio.domain.batch.listener.BatchMetricsListener;
import site.kkokkio.domain.batch.listener.LogStepListener;
import site.kkokkio.domain.source.dto.SearchStatsDto;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.scheduler.HourScheduler;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class SearchNewsStepConfigTest {

	private static final String STEP_NAME = BatchConstants.SEARCH_NEWS_STEP;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private Job testJob;

	@MockitoBean
	private HourScheduler hourScheduler;

	@MockitoBean
	private SourceService sourceService;

	@MockitoBean
	private BatchMetricsListener metricsListener;

	@MockitoBean
	private LogStepListener errListener;

	@BeforeEach
	void resetMocks() {
		jobLauncherTestUtils.setJob(testJob);
		reset(sourceService, metricsListener, errListener);
	}

	@TestConfiguration
	static class TestJobConfig {
		@Autowired
		private JobRepository jobRepository;

		@Bean
		public Job testJob(Step searchNewsStep) {
			return new JobBuilder("testJob", jobRepository)
				.start(searchNewsStep)
				.build();
		}
	}

	@Test
	@DisplayName("searchNewsStep 성공")
	void testSuccess() throws Exception {
		// given
		when(sourceService.searchNews()).thenReturn(new SearchStatsDto(5, 1));

		// when
		JobExecution exec = jobLauncherTestUtils.launchStep(STEP_NAME);

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// JobExecutionContext 검증
		StepExecution stepExec = exec.getStepExecutions().iterator().next();
		ExecutionContext stepCtx = stepExec.getExecutionContext();
		assertThat(stepCtx.getInt(ExecutionContextKeys.SC_NEWS_FETCHED)).isEqualTo(5);
		assertThat(stepCtx.getInt(ExecutionContextKeys.SC_NEWS_API_FAIL)).isEqualTo(1);

		// sourceService 호출 검증
		verify(sourceService, times(1)).searchNews();

		// Listener 호출 검증
		verify(metricsListener, times(1)).beforeStep(any(StepExecution.class));
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).beforeStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("searchNewsStep 성공: 빈 리스트 반환")
	void testEmptyList() throws Exception {
		// given
		when(sourceService.searchNews()).thenReturn(new SearchStatsDto(0, 0));

		// when
		JobExecution exec = jobLauncherTestUtils.launchStep(STEP_NAME);

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// JobExecutionContext 검증
		StepExecution stepExec = exec.getStepExecutions().iterator().next();
		ExecutionContext stepCtx = stepExec.getExecutionContext();
		assertThat(stepCtx.getInt(ExecutionContextKeys.SC_NEWS_FETCHED)).isZero();
		assertThat(stepCtx.getInt(ExecutionContextKeys.SC_NEWS_API_FAIL)).isZero();

		// sourceService 호출 검증
		verify(sourceService, times(1)).searchNews();

		// Listener 호출 검증
		verify(metricsListener, times(1)).beforeStep(any(StepExecution.class));
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).beforeStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("searchNewsStep 실패: Service 예외 발생")
	void testFailure() throws Exception {
		// given
		when(sourceService.searchNews())
			.thenThrow(new RuntimeException("API 오류"));

		// when
		JobExecution exec = jobLauncherTestUtils.launchStep(STEP_NAME);

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);

		// JobExecutionContext 검증
		ExecutionContext jobCtx = exec.getExecutionContext();
		assertThat(jobCtx.containsKey(ExecutionContextKeys.SC_NEWS_FETCHED)).isFalse();
		assertThat(jobCtx.containsKey(ExecutionContextKeys.SC_NEWS_API_FAIL)).isFalse();

		// sourceService 호출 검증
		verify(sourceService, times(1)).searchNews();

		// Listener 호출 검증 - FAILED
		verify(metricsListener, times(1)).beforeStep(any(StepExecution.class));
		verify(metricsListener, times(1)).afterStep(argThat(stepExec ->
			stepExec.getStatus() == BatchStatus.FAILED
		));
		verify(errListener, times(1)).beforeStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(argThat(stepExec ->
			stepExec.getStatus() == BatchStatus.FAILED
		));
	}

	@Test
	@DisplayName("searchNewsStep 재시도 성공(allowStartIfComplete=true): 2회 연속 실행 시도 가능")
	void testAllowStartIfComplete() throws Exception {
		// given
		when(sourceService.searchNews())
			.thenReturn(new SearchStatsDto(2, 0));

		// when&then: 첫 번째 실행
		JobExecution first = jobLauncherTestUtils.launchStep(STEP_NAME);
		assertThat(first.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// when&then: 두 번째 실행
		JobExecution second = jobLauncherTestUtils.launchStep(STEP_NAME);
		assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// sourceService 두 번 호출
		verify(sourceService, times(2)).searchNews();
	}
}