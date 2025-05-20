package site.kkokkio.domain.batch;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static site.kkokkio.domain.batch.context.BatchConstants.*;
import static site.kkokkio.domain.batch.context.ExecutionContextKeys.*;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import site.kkokkio.domain.batch.context.BatchConstants;
import site.kkokkio.domain.batch.context.ExecutionContextKeys;
import site.kkokkio.domain.keyword.dto.NoveltyStatsDto;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.keyword.service.TrendsService;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.dto.SearchStatsDto;
import site.kkokkio.domain.source.service.SourceService;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class TrendToPostJobConfigTest {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private Job trendToPostJob;

	@MockitoBean
	private TrendsService trendsService;
	@MockitoBean
	private SourceService sourceService;
	@MockitoBean
	private KeywordMetricHourlyService keywordMetricHourlyService;
	@MockitoBean
	private PostService postService;

	@BeforeEach
	void setUp() {
		when(trendsService.getTrendingKeywordsFromRss())
			.thenReturn(List.of(Keyword.builder().id(101L).text("테스트키워드").build()));
		when(sourceService.searchNews())
			.thenReturn(new SearchStatsDto(1, 0));
		when(sourceService.searchYoutube())
			.thenReturn(new SearchStatsDto(1, 0));
		when(keywordMetricHourlyService.evaluateNovelty(any()))
			.thenReturn(new NoveltyStatsDto(1, List.of(1L)));
		when(postService.generatePosts(any()))
			.thenReturn(List.of(1L));
		when(postService.cacheCardViews(any(), any(), any()))
			.thenReturn(1);
	}

	@Test
	@DisplayName("trendToPostJob: 기본 흐름 (postableIds 존재)")
	void testTrendToPostJob_Success() throws Exception {
		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchJob();

		// then
		// job completed
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		List<String> allStepNames = jobExecution.getStepExecutions().stream()
			.map(StepExecution::getStepName)
			.toList();

		assertThat(allStepNames).containsExactlyInAnyOrder(
			FETCH_KEYWORDS_STEP,
			SEARCH_NEWS_STEP,
			SEARCH_VIDEOS_STEP,
			EVALUATE_NOVELTY_STEP,
			GENERATE_POST_STEP,
			CACHE_POST_STEP
		);

		// all steps completed
		jobExecution.getStepExecutions().forEach(se ->
			assertThat(se.getStatus()).isEqualTo(BatchStatus.COMPLETED)
		);

		// mocking service test
		List<Long> topIds = (List<Long>)jobExecution.getExecutionContext().get(JC_TOP_KEYWORD_IDS);
		assertThat(topIds).containsExactly(101L);

		// JC_NO_POST_NEEDED flag 확인
		assertThat(jobExecution.getExecutionContext()
			.get(ExecutionContextKeys.JC_NO_POST_NEEDED))
			.isEqualTo(Boolean.FALSE);

		// service 호출 검증
		verify(postService, times(1)).generatePosts(any());
		verify(postService, times(1)).cacheCardViews(any(), any(), any());

	}

	@Test
	@DisplayName("trendToPostJob: generatePosts 스킵 (postableIds 빈 리스트)")
	void testTrendToPostJob_NoPostNeeded() throws Exception {
		// given
		// postableIds empty 로 분기
		when(keywordMetricHourlyService.evaluateNovelty(any()))
			.thenReturn(new NoveltyStatsDto(2, List.of()));

		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchJob();

		// then
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// steps completed(GENERATE_POST_STEP 제외)
		List<String> names = jobExecution.getStepExecutions().stream()
			.map(StepExecution::getStepName)
			.collect(Collectors.toList());
		assertThat(names).contains(BatchConstants.CACHE_POST_STEP)
			.doesNotContain(BatchConstants.GENERATE_POST_STEP);

		// JC_NO_POST_NEEDED flag 확인
		assertThat(jobExecution.getExecutionContext()
			.get(ExecutionContextKeys.JC_NO_POST_NEEDED))
			.isEqualTo(Boolean.TRUE);

		// service 호출 검증
		verify(postService, never()).generatePosts(any());
		verify(postService, never()).cacheCardViews(any(), any(), any());
	}

}