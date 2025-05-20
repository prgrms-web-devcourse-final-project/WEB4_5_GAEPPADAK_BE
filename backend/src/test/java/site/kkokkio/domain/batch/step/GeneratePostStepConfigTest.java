package site.kkokkio.domain.batch.step;

import static org.assertj.core.api.Assertions.*;
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
import site.kkokkio.domain.batch.context.ExecutionContextKeys;
import site.kkokkio.domain.batch.listener.BatchMetricsListener;
import site.kkokkio.domain.batch.listener.LogStepListener;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.global.scheduler.HourScheduler;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class GeneratePostStepConfigTest {

	private static final String STEP_NAME = BatchConstants.GENERATE_POST_STEP;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private Job testJob;

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
		jobLauncherTestUtils.setJob(testJob);
		reset(postService, metricsListener, errListener);
	}

	@TestConfiguration
	static class TestJobConfig {
		@Autowired
		private JobRepository jobRepository;

		@Bean
		public Job testJob(Step generatePostStep) {
			return new JobBuilder("testJob", jobRepository)
				// Job 시작 전, JC_POSTABLE_KEYWORD_IDS 주입
				.listener(new JobExecutionListener() {
					@Override
					public void beforeJob(JobExecution jobExecution) {
						jobExecution.getExecutionContext()
							.put(JC_POSTABLE_KEYWORD_IDS, List.of(11L, 22L));
					}
				})
				.start(generatePostStep)
				.build();
		}
	}

	@Test
	@DisplayName("GeneratePostStep 성공")
	void testGeneratePost_Success() throws Exception {
		// given
		when(postService.generatePosts(List.of(11L, 22L)))
			.thenReturn(List.of(101L, 102L));

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// JobExecutionContext 검증
		@SuppressWarnings("unchecked")
		List<Long> createdIds = (List<Long>)exec.getExecutionContext()
			.get(ExecutionContextKeys.JC_NEW_POST_IDS);
		assertThat(createdIds).containsExactly(101L, 102L);

		// StepExecutionContext 검증
		StepExecution se = exec.getStepExecutions().iterator().next();
		int created = se.getExecutionContext()
			.getInt(ExecutionContextKeys.SC_POST_CREATED);
		assertThat(created).isEqualTo(2);

		// service 호출 검증
		verify(postService, times(1)).generatePosts(List.of(11L, 22L));

		// listener 호출 검증
		verify(metricsListener, times(1)).beforeStep(any(StepExecution.class));
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).beforeStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("GeneratePostStep 성공: generatePosts 빈 리스트 반환")
	void testGeneratePost_Empty() throws Exception {
		// given
		when(postService.generatePosts(anyList()))
			.thenReturn(List.of());

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// jobExecutionContext 검증
		@SuppressWarnings("unchecked")
		List<Long> createdIds = (List<Long>)exec.getExecutionContext()
			.get(ExecutionContextKeys.JC_NEW_POST_IDS);
		assertThat(createdIds).isEmpty();

		// stepExecutionContext 검증
		StepExecution se = exec.getStepExecutions().iterator().next();
		int created = se.getExecutionContext()
			.getInt(ExecutionContextKeys.SC_POST_CREATED);
		assertThat(created).isZero();

		// service 호출 검증
		verify(postService, times(1)).generatePosts(List.of(11L, 22L));

		// listener 호출 검증
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("GeneratePostStep 실패: generatePosts 예외 발생")
	void testGeneratePost_Failure() throws Exception {
		// given
		when(postService.generatePosts(anyList()))
			.thenThrow(new RuntimeException("error"));

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);

		// JobExecutionContext 검증
		assertThat(exec.getExecutionContext().containsKey(JC_NEW_POST_IDS)).isFalse();

		// StepExecutionContext 검증
		assertThat(exec.getStepExecutions().iterator().next().getExecutionContext()
			.containsKey(SC_POST_CREATED)).isFalse();

		// service 호출 검증
		verify(postService, times(1)).generatePosts(anyList());

		// listener 호출 검증
		verify(errListener).afterStep(argThat(stepExec ->
			stepExec.getStatus() == BatchStatus.FAILED
		));
	}

	@Test
	@DisplayName("GeneratePostStep 재시도 성공(allowStartIfComplete=true): 2회 연속 실행 시도 가능")
	void testAllowStartIfComplete() throws Exception {
		// given
		when(postService.generatePosts(anyList()))
			.thenReturn(List.of(1L));

		// when&then: 첫 번째 실행
		JobExecution first = jobLauncherTestUtils.launchJob();
		assertThat(first.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// when&then: 두 번째 실행
		JobExecution second = jobLauncherTestUtils.launchJob();
		assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// trendsService 두 번 호출
		verify(postService, times(2)).generatePosts(anyList());
	}
}