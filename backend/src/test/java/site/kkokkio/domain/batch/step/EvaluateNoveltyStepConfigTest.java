package site.kkokkio.domain.batch.step;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.*;
import static site.kkokkio.domain.batch.context.ExecutionContextKeys.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import site.kkokkio.domain.batch.context.BatchConstants;
import site.kkokkio.domain.batch.listener.BatchMetricsListener;
import site.kkokkio.domain.batch.listener.LogStepListener;
import site.kkokkio.domain.keyword.dto.NoveltyStatsDto;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.global.scheduler.HourScheduler;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class EvaluateNoveltyStepConfigTest {

	private static final String STEP_NAME = BatchConstants.EVALUATE_NOVELTY_STEP;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private Job testJob;

	@MockitoBean
	private KeywordMetricHourlyService keywordMetricHourlyService;

	@MockitoBean
	private BatchMetricsListener metricsListener;

	@MockitoBean
	private LogStepListener errListener;

	@MockitoBean
	private HourScheduler hourScheduler;

	@BeforeEach
	void setUp() {
		jobLauncherTestUtils.setJob(testJob);
		reset(keywordMetricHourlyService, metricsListener, errListener, hourScheduler);
	}

	@TestConfiguration
	static class TestJobConfig {
		@Autowired
		private JobRepository jobRepository;

		@Bean
		public Job testJob(Step evaluateNoveltyStep) {
			return new JobBuilder("testJob", jobRepository)
				// Job 시작 전, JC_TOP_KEYWORD_IDS 주입
				.listener(new JobExecutionListener() {
					@Override
					public void beforeJob(JobExecution jobExecution) {
						jobExecution.getExecutionContext()
							.put(JC_TOP_KEYWORD_IDS, List.of(7L, 8L, 9L));
					}
				})
				.start(evaluateNoveltyStep)
				.build();
		}
	}

	@Test
	@DisplayName("evalutateNoveltyStep 성공")
	void testEvaluateNovelty_Success() throws Exception {
		// given
		NoveltyStatsDto dto = mock(NoveltyStatsDto.class);
		when(dto.lowVariationCount()).thenReturn(2);
		when(dto.postableIds()).thenReturn(List.of(11L, 22L));
		when(keywordMetricHourlyService.evaluateNovelty(anyList()))
			.thenReturn(dto);

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// JobExecutionContext 검증
		var jobCtx = exec.getExecutionContext();
		@SuppressWarnings("unchecked")
		List<Long> postable = (List<Long>)jobCtx.get(JC_POSTABLE_KEYWORD_IDS);
		int count = jobCtx.getInt(JC_POSTABLE_KEYWORD_COUNT);
		assertThat(postable).containsExactly(11L, 22L);
		assertThat(count).isEqualTo(2);

		// StepExecutionContext 검증
		StepExecution se = exec.getStepExecutions().iterator().next();
		int skipped = se.getExecutionContext().getInt(SC_NOVELTY_SKIPPED);
		assertThat(skipped).isEqualTo(2);

		// service 호출 검증
		verify(keywordMetricHourlyService, times(1)).evaluateNovelty(anyList());

		// listener 호출 검증
		verify(metricsListener, times(1)).beforeStep(any(StepExecution.class));
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).beforeStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("evalutateNoveltyStep 성공: 빈 데이터")
	void testEvaluateNovelty_Empty() throws Exception {
		// given
		NoveltyStatsDto dto = mock(NoveltyStatsDto.class);
		when(dto.lowVariationCount()).thenReturn(0);
		when(dto.postableIds()).thenReturn(List.of());
		when(keywordMetricHourlyService.evaluateNovelty(anyList()))
			.thenReturn(dto);

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// JobExecutionContext 검증
		var jobCtx = exec.getExecutionContext();
		@SuppressWarnings("unchecked")
		List<Long> postable = (List<Long>)jobCtx.get(JC_POSTABLE_KEYWORD_IDS);
		int count = jobCtx.getInt(JC_POSTABLE_KEYWORD_COUNT);
		assertThat(postable).isEmpty();
		assertThat(count).isZero();

		// StepExecutionContext 검증
		int skipped = exec.getStepExecutions().iterator().next()
			.getExecutionContext()
			.getInt(SC_NOVELTY_SKIPPED);
		assertThat(skipped).isZero();

		// service 호출 검증
		verify(keywordMetricHourlyService, times(1)).evaluateNovelty(anyList());

		// listener 호출 검증
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("evalutateNoveltyStep 실패: 서비스 예외")
	void testEvaluateNovelty_Failure() throws Exception {
		// given
		when(keywordMetricHourlyService.evaluateNovelty(anyList()))
			.thenThrow(new RuntimeException("error"));

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);

		// Context 검증
		assertThat(exec.getExecutionContext().containsKey(JC_TOP_KEYWORD_IDS)).isTrue();
		assertThat(exec.getExecutionContext().containsKey(JC_POSTABLE_KEYWORD_IDS)).isFalse();
		assertThat(exec.getExecutionContext().containsKey(JC_POSTABLE_KEYWORD_COUNT)).isFalse();

		// listener 호출 검증
		verify(metricsListener, times(1)).afterStep(argThat(se ->
			se.getStatus() == BatchStatus.FAILED
		));
		verify(errListener, times(1)).afterStep(argThat(se ->
			se.getStatus() == BatchStatus.FAILED
		));
	}
}