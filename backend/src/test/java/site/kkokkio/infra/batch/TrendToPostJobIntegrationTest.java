package site.kkokkio.infra.batch;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import site.kkokkio.domain.batch.context.BatchConstants;
import site.kkokkio.domain.batch.context.ExecutionContextKeys;
import site.kkokkio.domain.batch.context.JobParameterKeys;
import site.kkokkio.domain.keyword.port.out.TrendsPort;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.keyword.service.KeywordService;
import site.kkokkio.domain.keyword.service.TrendsService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.domain.post.port.out.AiSummaryPort;
import site.kkokkio.domain.post.repository.PostKeywordRepository;
import site.kkokkio.domain.post.repository.PostMetricHourlyRepository;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.dto.NewsDto;
import site.kkokkio.domain.source.dto.VideoDto;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.port.out.NewsApiPort;
import site.kkokkio.domain.source.port.out.VideoApiPort;
import site.kkokkio.domain.source.repository.KeywordSourceRepository;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.domain.source.repository.SourceRepository;
import site.kkokkio.domain.source.service.OpenGraphService;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.auth.AuthChecker;
import site.kkokkio.global.auth.CustomUserDetailsService;
import site.kkokkio.global.config.SecurityConfig;
import site.kkokkio.global.util.JwtUtils;
import site.kkokkio.infra.ai.AiType;
import site.kkokkio.infra.google.trends.dto.KeywordInfo;

@SpringBootTest
@Import(SecurityConfig.class)
@SpringBatchTest
@AutoConfigureTestDatabase
public class TrendToPostJobIntegrationTest {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@MockitoBean
	AuthChecker authChecker;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private JwtUtils jwtUtils;

	@Autowired
	private Job trendToPostJob;

	@MockitoBean
	private TrendsPort trendsPort;

	@MockitoBean
	private NewsApiPort newsApiPort;

	@MockitoBean
	private VideoApiPort videoApiPort;

	@MockitoBean
	private AiSummaryPort aiSummaryPort;

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@MockitoBean
	private ObjectMapper objectMapper;

	@MockitoBean
	private MemberService memberService;

	@Autowired
	TrendsService trendsService;

	@Autowired
	SourceService sourceService;

	@Autowired
	KeywordMetricHourlyService keywordMetricHourlyService;

	@Autowired
	PostService postService;

	@Autowired
	KeywordService keywordService;

	@Autowired
	OpenGraphService openGraphService;

	@Autowired
	KeywordRepository keywordRepository;

	@Autowired
	KeywordMetricHourlyRepository keywordMetricHourlyRepository;

	@Autowired
	SourceRepository sourceRepository;

	@Autowired
	KeywordSourceRepository keywordSourceRepository;

	@Autowired
	PostRepository postRepository;

	@Autowired
	PostKeywordRepository postKeywordRepository;

	@Autowired
	PostMetricHourlyRepository postMetricHourlyRepository;

	@Autowired
	PostSourceRepository postSourceRepository;

	// 테스트 시 사용할 기본 Job Parameter 설정
	private JobParameters defaultJobParameters;
	@Autowired
	private RedisTemplate<Object, Object> redisTemplate;
	@Autowired
	private Job job;

	@BeforeEach
	void setUp() {
		// Job Repository의 이전 실행 데이터 정리
		jobRepositoryTestUtils.removeJobExecutions();

		// 테스트 시 사용할 기본 Job Parameter 설정
		defaultJobParameters = new JobParametersBuilder()
			.addLocalDateTime(JobParameterKeys.JP_RUNTIME, LocalDateTime.now())
			.toJobParameters();

		// jobLauncherTestUtils에 테스트할 Job 설정
		jobLauncherTestUtils.setJob(this.trendToPostJob);

		// RedisTemplate 및 관련 오퍼레이션 Mocking
		ValueOperations<String, String> mockValueOps = Mockito.mock(ValueOperations.class);

		// 메서드 'thenReturn' 해결 오류를 회피 시도합니다.
		doReturn(mockValueOps).when(redisTemplate).opsForValue();

		// RedisTemplate opsForValue().set() 메서드 Mocking. 어떤 키, 값, TTL이 오든 성공하도록 설정
		lenient().doNothing().when(mockValueOps).set(anyString(), anyString(), any(Duration.class));

		// DTO 객체가 JSON 문자열로 변환될 때의 동작 Mocking
		try {
			lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"json\"}");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@DisplayName("Job 전체 성공")
	void testJob_Success() throws Exception {
		/// given
		// 1. Google Trends RSS Port Mock하여 N개의 KeywordInfo 반환
		int keywordCount = 10;
		List<KeywordInfo> mockKeywordInfos = IntStream.rangeClosed(1, keywordCount)
			.mapToObj(id -> KeywordInfo.builder().text("trending" + id).volume(1000 - id).build())
			.collect(Collectors.toList());
		when(trendsPort.fetchTrendingKeywords()).thenReturn(mockKeywordInfos);

		// 2. Naver News API Port Mock하여 각 키워드별 NewsDto 목록 반환
		int newsCountKeyword = 5;
		List<NewsDto> mockNewsList = IntStream.rangeClosed(1, newsCountKeyword)
			.mapToObj(id -> NewsDto.builder()
				.title("뉴스 제목" + id).link("http://news.com/" + id).originalLink("http://orig.news.com/" + id)
				.description("뉴스 요약" + id).pubDate(LocalDateTime.now().minusMinutes(id)).build())
			.collect(Collectors.toList());
		when(newsApiPort.fetchNews(anyString(), anyInt(), anyInt(), anyString()))
			.thenReturn(Mono.just(mockNewsList));

		// 3. Youtube Data API Port Mock하여 각 키워드별 VideoDto 목록 반환
		int videoCountKeyword = 3;
		List<VideoDto> mockVideoList = IntStream.rangeClosed(1, videoCountKeyword)
			.mapToObj(id -> VideoDto.builder()
				.url("http://youtube.com/watch?v=" + id).title("영상 제목" + id)
				.publishedAt(LocalDateTime.now().minusHours(1).minusMinutes(id)).build())
			.collect(Collectors.toList());
		when(videoApiPort.fetchVideos(anyString(), anyInt()))
			.thenReturn(Mono.just(mockVideoList));

		// 4. AI Summary Port Mock하여 LLM 요약 결과 JSON 문자열 반환
		String mockAiResponseJson = "{\"title\": \"요약된 포스트 제목\", \"summary\": \"AI가 요약한 포스트 내용\"}";
		when(aiSummaryPort.summarize(any(AiType.class), anyString()))
			.thenReturn(CompletableFuture.completedFuture(mockAiResponseJson));

		/// when
		// Job 실행
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(defaultJobParameters);

		/// then
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// 각 Step의 실행 상태 검증
		assertThat(jobExecution.getStepExecutions())
			.extracting(StepExecution::getStepName, StepExecution::getExitStatus)
			.containsExactlyInAnyOrder(
				tuple(BatchConstants.FETCH_KEYWORDS_STEP, new ExitStatus("COMPLETED")),
				tuple(BatchConstants.SEARCH_SOURCES_FLOW, new ExitStatus("COMPLETED")),
				tuple(BatchConstants.SEARCH_NEWS_STEP, new ExitStatus("COMPLETED")),
				tuple(BatchConstants.SEARCH_VIDEOS_STEP, new ExitStatus("COMPLETED")),
				tuple(BatchConstants.EVALUATE_NOVELTY_STEP, new ExitStatus("COMPLETED")),
				tuple(BatchConstants.GENERATE_POST_STEP, new ExitStatus("COMPLETED")),
				tuple(BatchConstants.CACHE_POST_STEP, new ExitStatus("COMPLETED"))
			);

		// Execution Context 값 검증
		// fetchTrendingKeywordsStep에서 Job EC에 저장한 값
		assertThat(jobExecution.getExecutionContext()
			.get(ExecutionContextKeys.JC_TOP_COUNT)).isEqualTo(keywordCount);
		assertThat(jobExecution.getExecutionContext()
			.get(ExecutionContextKeys.JC_TOP_IDS)).isInstanceOf(List.class);
		assertThat((List<Long>)jobExecution.getExecutionContext().get(ExecutionContextKeys.JC_TOP_IDS)).hasSize(
			keywordCount);

		// evaluateNoveltyStep에서 Job EC에 저장한 값
		assertThat(jobExecution.getExecutionContext().get(ExecutionContextKeys.JC_POSTABLE_COUNT)).isEqualTo(
			keywordCount);
		assertThat(jobExecution.getExecutionContext().get(ExecutionContextKeys.JC_POSTABLE_IDS)).isInstanceOf(
			List.class);
		assertThat((List<Long>)jobExecution.getExecutionContext().get(ExecutionContextKeys.JC_POSTABLE_IDS)).hasSize(
			keywordCount);
		assertThat(jobExecution.getExecutionContext().get(ExecutionContextKeys.JC_NO_POST_NEEDED)).isEqualTo(false);

		// generatePostStep에서 Job EC에 저장한 값
		assertThat(jobExecution.getExecutionContext().get(ExecutionContextKeys.JC_NEW_POST_IDS)).isInstanceOf(
			List.class);
		assertThat((List<Long>)jobExecution.getExecutionContext().get(ExecutionContextKeys.JC_NEW_POST_IDS)).hasSize(
			keywordCount);

		// searchNewsStep의 SC_NEWS_FETCHED 검증
		assertThat(jobExecution.getStepExecutions().stream()
			.filter(se -> se.getStepName().equals(BatchConstants.SEARCH_NEWS_STEP))
			.findFirst().get().getExecutionContext().getInt(ExecutionContextKeys.SC_NEWS_FETCHED))
			.isEqualTo(keywordCount * newsCountKeyword);

		// searchVideosStep의 SC_VIDEO_FETCHED 검증
		assertThat(jobExecution.getStepExecutions().stream()
			.filter(se -> se.getStepName().equals(BatchConstants.SEARCH_VIDEOS_STEP))
			.findFirst().get().getExecutionContext().getInt(ExecutionContextKeys.SC_VIDEO_FETCHED))
			.isEqualTo(keywordCount * videoCountKeyword);

		// evaluateNoveltyStep의 SC_NOVELTY_SKIPPED 검증
		assertThat(jobExecution.getStepExecutions().stream()
			.filter(se -> se.getStepName().equals(BatchConstants.EVALUATE_NOVELTY_STEP))
			.findFirst().get().getExecutionContext().getInt(ExecutionContextKeys.SC_NOVELTY_SKIPPED))
			.isEqualTo(0);

		// generatePostStep의 SC_POST_CREATED 검증
		assertThat(jobExecution.getStepExecutions().stream()
			.filter(se -> se.getStepName().equals(BatchConstants.GENERATE_POST_STEP))
			.findFirst().get().getExecutionContext().getInt(ExecutionContextKeys.SC_POST_CREATED))
			.isEqualTo(keywordCount);

		// cachePostStep의 SC_CACHE_SIZE 검증
		assertThat(jobExecution.getStepExecutions().stream()
			.filter(se -> se.getStepName().equals(BatchConstants.CACHE_POST_STEP))
			.findFirst().get().getExecutionContext().getInt(ExecutionContextKeys.SC_CACHE_SIZE))
			.isEqualTo(keywordCount);

		// Keyword 테이블 검증 trendsService.getTrendingKeywordsFromRss()에서 반환된 개수만큼 저장 예상
		assertThat(keywordRepository.count()).isEqualTo(keywordCount);

		// KeywordMetricHourly 테이블 검증: 각 키워드에 대해 1개씩 저장 예상
		assertThat(keywordMetricHourlyRepository.count()).isEqualTo(keywordCount);

		// Source 테이블 검증: 검색된 모든 Source 저장 예상 (중복 제거 후)
		assertThat(sourceRepository.count()).isEqualTo(keywordCount * (newsCountKeyword + videoCountKeyword));

		// KeywordSource 테이블 검증: 각 Source와 Keyword 매핑 저장 예상
		assertThat(keywordSourceRepository.count()).isEqualTo(keywordCount * (newsCountKeyword + videoCountKeyword));

		// Post 테이블 검증 Postable 키워드 수만큼 생성 예상 (Happy Path이므로 keywordCount와 동일)
		assertThat(postRepository.count()).isEqualTo(keywordCount);

		// PostMetricHourly 테이블 검증 각 Post에 대해 1개씩 생성 예상
		assertThat(postMetricHourlyRepository.count()).isEqualTo(keywordCount);

		// PostSource 테이블 검증: 각 Post와 연결된 Source 수만큼 저장 예상
		assertThat(postSourceRepository.count()).isEqualTo(keywordCount * (newsCountKeyword + videoCountKeyword));

		// TrendsService.getTrendingKeywordsFromRss() 호출 검증
		verify(trendsPort).fetchTrendingKeywords();

		// SourceService.searchNews() 호출 검증
		verify(sourceService).searchNews();

		// SourceService.searchYoutube() 호출 검증
		verify(sourceService).searchYoutube();

		// SourceService 내부에서 KeywordMetricHourlyService.findHourlyMetrics() 호출 검증
		verify(keywordMetricHourlyService, times(2)).findHourlyMetrics();

		// SourceService 내부에서 NewsApiPort/VideoApiPort 호출 검증
		verify(newsApiPort, times(keywordCount)).fetchNews(anyString(), anyInt(), anyInt(), anyString());
		verify(videoApiPort, times(keywordCount)).fetchVideos(anyString(), anyInt());

		// evaluateNoveltyStep -> KeywordMetricHourlyService.evaluateNovelty() 호출 검증
		verify(keywordMetricHourlyService).evaluateNovelty(anyList());

		// generatePostStep -> PostService.generatePosts() 호출 검증
		verify(postService).generatePosts(anyList());

		// PostService.generatePosts 내부에서 AiSummaryPort.summarize() 호출 검증
		verify(aiSummaryPort, times(keywordCount)).summarize(any(AiType.class), anyString());

		// generatePostStep 내부 PostService.linkSourcesToPost()에서 Repository insertIgnoreAll 호출 검증
		verify(postKeywordRepository, times(keywordCount)).insertIgnoreAll(anyList());
		verify(postSourceRepository, times(keywordCount)).insertIgnoreAll(anyList());

		// SourceService에서 Source/KeywordSource 저장 시 한 번씩 호출
		verify(sourceRepository).insertIgnoreAll(anyList());
		verify(keywordSourceRepository).insertIgnoreAll(anyList());

		// cachePostStep -> PostService.cacheCardViews() 호출 검증
		verify(postService).cacheCardViews(anyList(), anyList(), any(Duration.class));

		// cachePostStep 내부 PostService.cachePostCardView()에서 Redis set 호출 검증
		verify(redisTemplate.opsForValue(), times(keywordCount)).set(anyString(), anyString(), any(Duration.class));

		// OpenGraphService.enrichAsync 호출 검증
		verify(openGraphService, times(keywordCount * (newsCountKeyword + videoCountKeyword)))
			.enrichAsync(any(Source.class));
	}
}
