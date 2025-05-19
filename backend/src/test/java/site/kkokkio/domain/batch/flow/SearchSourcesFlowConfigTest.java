package site.kkokkio.domain.batch.flow;

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
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
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
import site.kkokkio.domain.source.dto.SearchStatsDto;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.scheduler.HourScheduler;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class SearchSourcesFlowConfigTest {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private Job testFlowJob;

	@MockitoBean
	private SourceService sourceService;

	@MockitoBean
	private BatchMetricsListener metricsListener;

	@MockitoBean
	private LogStepListener errListener;

	@MockitoBean
	private HourScheduler hourScheduler;

	@BeforeEach
	void setUp() {
		jobLauncherTestUtils.setJob(testFlowJob);
		reset(sourceService, metricsListener, errListener, hourScheduler);
	}

	@TestConfiguration
	static class TestFlowJobConfig {
		@Autowired
		private JobRepository jobRepository;

		@Bean
		public Job testFlowJob(Flow searchSourcesFlow) {
			return new JobBuilder("testFlowJob", jobRepository)
				.start(searchSourcesFlow)
				.end()
				.build();
		}
	}

	@Test
	@DisplayName("searchSourcesFlow 성공: 뉴스/영상 검색 모두 성공")
	void testFlow_Success() throws Exception {
		// given
		when(sourceService.searchNews()).thenReturn(new SearchStatsDto(5, 1));
		when(sourceService.searchYoutube()).thenReturn(new SearchStatsDto(3, 0));

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// 두 스텝이 실행되었는지, 이름과 상태 검증
		List<StepExecution> steps = exec.getStepExecutions().stream().toList();
		assertThat(steps).extracting(StepExecution::getStepName)
			.containsExactlyInAnyOrder(
				BatchConstants.SEARCH_NEWS_STEP,
				BatchConstants.SEARCH_VIDEOS_STEP
			);
		steps.forEach(se -> assertThat(se.getStatus()).isEqualTo(BatchStatus.COMPLETED));

		// 뉴스 Step Context 검증
		StepExecution news = steps.stream()
			.filter(se -> se.getStepName().equals(BatchConstants.SEARCH_NEWS_STEP))
			.findFirst().get();
		assertThat(news.getExecutionContext().getInt(ExecutionContextKeys.SC_NEWS_FETCHED)).isEqualTo(5);
		assertThat(news.getExecutionContext().getInt(ExecutionContextKeys.SC_NEWS_API_FAIL)).isEqualTo(1);

		// 영상 Step Context 검증
		StepExecution video = steps.stream()
			.filter(se -> se.getStepName().equals(BatchConstants.SEARCH_VIDEOS_STEP))
			.findFirst().get();
		assertThat(video.getExecutionContext().getInt(ExecutionContextKeys.SC_VIDEO_FETCHED)).isEqualTo(3);
		assertThat(video.getExecutionContext().getInt(ExecutionContextKeys.SC_VIDEO_API_FAIL)).isEqualTo(0);

		// Service 호출 검증
		verify(sourceService, times(1)).searchNews();
		verify(sourceService, times(1)).searchYoutube();
	}

	@Test
	@DisplayName("searchSourcesFlow 부분 실패: 뉴스 실패 + 영상 성공")
	void testFlow_NewsFailure() throws Exception {
		// given
		when(sourceService.searchNews()).thenThrow(new RuntimeException("news error"));
		when(sourceService.searchYoutube()).thenReturn(new SearchStatsDto(2, 0));

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);

		// step 상태 검증
		List<StepExecution> steps = exec.getStepExecutions().stream().toList();
		StepExecution news = steps.stream()
			.filter(se -> se.getStepName().equals(BatchConstants.SEARCH_NEWS_STEP))
			.findFirst().get();
		StepExecution video = steps.stream()
			.filter(se -> se.getStepName().equals(BatchConstants.SEARCH_VIDEOS_STEP))
			.findFirst().get();

		assertThat(news.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(video.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// 영상 Context 존재, 뉴스 Context empty
		assertThat(video.getExecutionContext().getInt(ExecutionContextKeys.SC_VIDEO_FETCHED)).isEqualTo(2);
		assertThat(video.getExecutionContext().getInt(ExecutionContextKeys.SC_VIDEO_API_FAIL)).isEqualTo(0);
		assertThat(news.getExecutionContext().containsKey(SC_NEWS_FETCHED)).isFalse();
		assertThat(news.getExecutionContext().containsKey(SC_NEWS_API_FAIL)).isFalse();

		// Service 호출 검증
		verify(sourceService, times(1)).searchNews();
		verify(sourceService, times(1)).searchYoutube();
	}

	@Test
	@DisplayName("searchSourcesFlow 실패: 뉴스/영상 모두 실패")
	void testFlow_AllFailure() throws Exception {
		//given
		when(sourceService.searchNews()).thenThrow(new RuntimeException("news error"));
		when(sourceService.searchYoutube()).thenThrow(new RuntimeException("video error"));

		// when
		JobExecution exec = jobLauncherTestUtils.launchJob();

		// then
		assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);

		// step 상태 검증
		exec.getStepExecutions().forEach(se ->
			assertThat(se.getStatus()).isEqualTo(BatchStatus.FAILED)
		);

		// service 호출 검증
		verify(sourceService, times(1)).searchNews();
		verify(sourceService, times(1)).searchYoutube();
	}
}
