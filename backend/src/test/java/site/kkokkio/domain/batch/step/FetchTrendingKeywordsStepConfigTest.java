package site.kkokkio.domain.batch.step;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
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
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.service.TrendsService;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class FetchTrendingKeywordsStepConfigTest {

	private static final String STEP_NAME = BatchConstants.FETCH_KEYWORDS_STEP;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@MockitoBean
	private TrendsService trendsService;

	@MockitoBean
	private BatchMetricsListener metricsListener;

	@MockitoBean
	private LogStepListener errListener;

	@BeforeEach
	void resetMocks() {
		reset(trendsService, metricsListener, errListener);
	}

	@Test
	@DisplayName("fetchTrendingKeywordsStep 성공: 2개 키워드 반환")
	void testSuccess() throws Exception {
		// given
		List<Keyword> keywordList = List.of(Keyword.builder().id(1L).text("A").build(),
			Keyword.builder().id(2L).text("B").build());
		when(trendsService.getTrendingKeywordsFromRss()).thenReturn(keywordList);

		// when
		JobExecution exec = jobLauncherTestUtils.launchStep(STEP_NAME);

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// JobExecutionContext 검증
		ExecutionContext jobCtx = exec.getExecutionContext();
		assertThat(jobCtx.containsKey(
			ExecutionContextKeys.JC_TOP_KEYWORD_IDS)).isTrue();
		assertThat(jobCtx.containsKey(
			ExecutionContextKeys.JC_TOP_KEYWORD_COUNT)).isTrue();

		@SuppressWarnings("unchecked")
		List<Long> ids = (List<Long>)jobCtx.get(ExecutionContextKeys.JC_TOP_KEYWORD_IDS);
		assertThat(ids).containsExactly(1L, 2L);
		assertThat(jobCtx.getInt(ExecutionContextKeys.JC_TOP_KEYWORD_COUNT)).isEqualTo(2);

		// trendsService 호출 검증
		verify(trendsService, times(1)).getTrendingKeywordsFromRss();

		// Listener 호출 검증
		verify(metricsListener, times(1)).beforeStep(any(StepExecution.class));
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).beforeStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("fetchTrendingKeywordsStep 성공: 빈 리스트 반환")
	void testEmptyList() throws Exception {
		// given
		when(trendsService.getTrendingKeywordsFromRss()).thenReturn(List.of());

		// when
		JobExecution exec = jobLauncherTestUtils.launchStep(STEP_NAME);

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// JobExecutionContext 검증
		ExecutionContext jobCtx = exec.getExecutionContext();
		assertThat(jobCtx.getInt(ExecutionContextKeys.JC_TOP_KEYWORD_COUNT)).isZero();

		@SuppressWarnings("unchecked")
		List<Long> ids = (List<Long>)jobCtx.get(ExecutionContextKeys.JC_TOP_KEYWORD_IDS);
		assertThat(ids).isEmpty();

		// trendsService 호출 검증
		verify(trendsService, times(1)).getTrendingKeywordsFromRss();

		// Listener 호출 검증
		verify(metricsListener, times(1)).beforeStep(any(StepExecution.class));
		verify(metricsListener, times(1)).afterStep(any(StepExecution.class));
		verify(errListener, times(1)).beforeStep(any(StepExecution.class));
		verify(errListener, times(1)).afterStep(any(StepExecution.class));
	}

	@Test
	@DisplayName("fetchTrendingKeywordsStep 실패: Service 예외 발생")
	void testFailure() throws Exception {
		// given
		when(trendsService.getTrendingKeywordsFromRss())
			.thenThrow(new RuntimeException("API 오류"));

		// when
		JobExecution exec = jobLauncherTestUtils.launchStep(STEP_NAME);

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);

		// JobExecutionContext 검증
		ExecutionContext jobCtx = exec.getExecutionContext();
		assertThat(jobCtx.containsKey(ExecutionContextKeys.JC_TOP_KEYWORD_IDS)).isFalse();
		assertThat(jobCtx.containsKey(ExecutionContextKeys.JC_TOP_KEYWORD_COUNT)).isFalse();

		// trendsService 호출 검증
		verify(trendsService, times(1)).getTrendingKeywordsFromRss();

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
	@DisplayName("fetchTrendingKeywordsStep 재시도 성공(allowStartIfComplete=true): 2회 연속 실행 시도 가능")
	void testAllowStartIfComplete() throws Exception {
		// given
		List<Keyword> keywordList = List.of(Keyword.builder().id(999L).text("X").build());
		when(trendsService.getTrendingKeywordsFromRss())
			.thenReturn(keywordList);

		// when&then: 첫 번째 실행
		JobExecution first = jobLauncherTestUtils.launchStep(STEP_NAME);
		assertThat(first.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// when&then: 두 번째 실행
		JobExecution second = jobLauncherTestUtils.launchStep(STEP_NAME);
		assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// trendsService 두 번 호출
		verify(trendsService, times(2)).getTrendingKeywordsFromRss();
	}
}