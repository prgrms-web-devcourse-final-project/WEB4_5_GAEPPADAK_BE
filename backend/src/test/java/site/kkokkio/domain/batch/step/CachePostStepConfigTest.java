package site.kkokkio.domain.batch.step;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import site.kkokkio.domain.batch.context.BatchConstants;
import site.kkokkio.domain.batch.context.ExecutionContextKeys;
import site.kkokkio.domain.batch.listener.BatchMetricsListener;
import site.kkokkio.domain.batch.listener.LogStepListener;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.global.scheduler.HourScheduler;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class CachePostStepConfigTest {

	private static final String STEP_NAME = BatchConstants.CACHE_POST_STEP;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private Step cachePostStep;

	@MockitoBean
	private PostService postService;

	@MockitoBean
	private BatchMetricsListener metricsListener;

	@MockitoBean
	private LogStepListener errListener;

	@MockitoBean
	private HourScheduler hourScheduler;

	@BeforeEach
	void setUp() {
		reset(postService, metricsListener, errListener, hourScheduler);
	}

	@Test
	@DisplayName("cachePostStep 성공: newPostIds null")
	void testCacheStep_NewPostIdsNull() throws Exception {
		// given
		Job job = new JobBuilder("cacheNullJob", jobRepository)
			.start(cachePostStep)
			.build();
		jobLauncherTestUtils.setJob(job);

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// given
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// stepExecutionContext 검증
		int cacheSize = exec.getStepExecutions().iterator().next()
			.getExecutionContext().getInt(ExecutionContextKeys.SC_CACHE_SIZE);
		assertThat(cacheSize).isZero();

		// service 호출 검증
		verify(postService, never()).cacheCardViews(anyList(), anyList(), any(Duration.class));

		// listener 호출 검증
		verify(metricsListener, times(1)).beforeStep(any(StepExecution.class));
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).beforeStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("cachePostStep 성공: newPostIds 빈 리스트")
	void testCacheStep_NewPostIdsEmpty() throws Exception {
		// given
		// Job 시작 전, JC_NEW_POST_IDS, JC_POSTABLE_KEYWORD_IDS 주입
		Job job = new JobBuilder("cacheEmptyJob", jobRepository)
			.listener(new JobExecutionListener() {
				@Override
				public void beforeJob(JobExecution jobExecution) {
					jobExecution.getExecutionContext()
						.put(ExecutionContextKeys.JC_NEW_POST_IDS, List.<Long>of());
					jobExecution.getExecutionContext()
						.put(ExecutionContextKeys.JC_POSTABLE_KEYWORD_IDS, List.of(9L, 8L));
				}
			})
			.start(cachePostStep)
			.build();
		jobLauncherTestUtils.setJob(job);

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// given
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// StepExecutionContext 검증
		StepExecution se = exec.getStepExecutions().iterator().next();
		int cacheSize = se.getExecutionContext().getInt(ExecutionContextKeys.SC_CACHE_SIZE);
		assertThat(cacheSize).isZero();

		// service 호출 검증
		verify(postService, never()).cacheCardViews(anyList(), anyList(), any(Duration.class));

		// listener 호출 검증
		verify(metricsListener, times(1)).beforeStep(any(StepExecution.class));
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).beforeStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("cachePostStep 성공: newPostIds 존재")
	void testCacheStep_Success() throws Exception {
		// given
		List<Long> newIds = List.of(11L, 22L);
		List<Long> kwIds = List.of(5L, 6L);
		when(postService.cacheCardViews(newIds, kwIds, Duration.ofHours(24)))
			.thenReturn(42);

		Job job = new JobBuilder("cacheSuccessJob", jobRepository)
			.listener(new JobExecutionListener() {
				@Override
				public void beforeJob(JobExecution jobExecution) {
					jobExecution.getExecutionContext()
						.put(ExecutionContextKeys.JC_NEW_POST_IDS, newIds);
					jobExecution.getExecutionContext()
						.put(ExecutionContextKeys.JC_POSTABLE_KEYWORD_IDS, kwIds);
				}
			})
			.start(cachePostStep)
			.build();
		jobLauncherTestUtils.setJob(job);

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// stepExecutionContext 검증
		StepExecution se = exec.getStepExecutions().iterator().next();
		int cacheSize = se.getExecutionContext().getInt(ExecutionContextKeys.SC_CACHE_SIZE);
		assertThat(cacheSize).isEqualTo(42);

		// service 호출 검증
		verify(postService, times(1)).cacheCardViews(newIds, kwIds, Duration.ofHours(24));

		// listener 호출 검증
		verify(metricsListener, times(1)).beforeStep(any(StepExecution.class));
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).beforeStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("cachePostStep 실패: 서비스 예외 발생")
	void testCacheStep_Failure() throws Exception {
		// given
		List<Long> newIds = List.of(99L);
		List<Long> kwIds = List.of(7L);
		when(postService.cacheCardViews(anyList(), anyList(), any(Duration.class)))
			.thenThrow(new RuntimeException("cache error"));

		Job job = new JobBuilder("cacheFailureJob", jobRepository)
			.listener(new JobExecutionListener() {
				@Override
				public void beforeJob(JobExecution jobExecution) {
					jobExecution.getExecutionContext()
						.put(ExecutionContextKeys.JC_NEW_POST_IDS, newIds);
					jobExecution.getExecutionContext()
						.put(ExecutionContextKeys.JC_POSTABLE_KEYWORD_IDS, kwIds);
				}
			})
			.start(cachePostStep)
			.build();
		jobLauncherTestUtils.setJob(job);

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// given
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);

		// StepExecutionContext
		StepExecution se = exec.getStepExecutions().iterator().next();
		assertThat(se.getExecutionContext().containsKey(ExecutionContextKeys.SC_CACHE_SIZE)).isFalse();

		// service 호출 검증
		verify(postService, times(1)).cacheCardViews(newIds, kwIds, Duration.ofHours(24));

		// listener 호출 검증
		verify(errListener).afterStep(argThat(s -> s.getStatus() == BatchStatus.FAILED));
	}
}