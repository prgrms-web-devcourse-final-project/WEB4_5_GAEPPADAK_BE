package site.kkokkio.domain.keyword.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyDto;
import site.kkokkio.domain.keyword.dto.NoveltyStatsDto;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class KeywordMetricHourlyServiceTest {
	@InjectMocks
	private KeywordMetricHourlyService keywordMetricHourlyService;

	@Mock
	private KeywordMetricHourlyRepository keywordMetricHourlyRepository;

	@Test
	@DisplayName("인기 키워드 조회 성공")
	void findHourlyMetricsTest() {
		// given
		List<KeywordMetricHourly> mockMetrics = new ArrayList<>();

		for (int i = 1; i <= 10; i++) {
			Keyword keyword = Keyword.builder().id((long)i).text("키워드 " + i).build();
			KeywordMetricHourlyId id = KeywordMetricHourlyId.builder()
				.keywordId((long)i)
				.bucketAt(LocalDateTime.now(ZoneId.of("UTC")).minusHours(i))
				.platform(Platform.GOOGLE_TREND)
				.build();
			KeywordMetricHourly metric = KeywordMetricHourly.builder()
				.id(id)
				.keyword(keyword)
				.volume(i * 10)
				.score(i * 10)
				.build();
			mockMetrics.add(metric);
		}
		when(keywordMetricHourlyRepository.findTop10HourlyMetricsClosestToNowNative(any(String.class))).thenReturn(
			mockMetrics);

		// when
		List<KeywordMetricHourlyDto> responses = keywordMetricHourlyService.findHourlyMetrics();

		// then
		assertThat(responses).hasSize(10);
		for (int i = 0; i < 10; i++) {
			KeywordMetricHourlyDto response = responses.get(i);
			assertThat(response.volume()).isEqualTo((i + 1) * 10);
			assertThat(response.score()).isEqualTo((i + 1) * 10);
			assertThat(response.platform()).isEqualTo(Platform.GOOGLE_TREND);
			assertThat(response.keywordId()).isEqualTo((long)(i + 1));
			assertThat(response.bucketAt()).isNotNull();
		}
	}

	@Test
	@DisplayName("인기 키워드 조회 실패 - 키워드 존재 X")
	void findHourlyMetricsTest_Exception_WhenNoKeywordsFound() {
		// given
		when(keywordMetricHourlyRepository.findTop10HourlyMetricsClosestToNowNative(
			any(String.class))).thenReturn(new ArrayList<>());

		// when
		// then
		ServiceException exception = assertThrows(ServiceException.class, () -> {
			keywordMetricHourlyService.findHourlyMetrics();
		});
		assertThat(exception.getCode()).isEqualTo("404");
		assertThat(exception.getMessage()).isEqualTo("키워드를 불러오지 못했습니다.");
	}

	@Test
	@DisplayName("인기 키워드 조회 실패 - null 반환")
	void findHourlyMetrics_Exception_WhenRepositoryReturnsNull() {
		// given
		when(keywordMetricHourlyRepository.findTop10HourlyMetricsClosestToNowNative(
			any(String.class))).thenReturn(null);

		// when
		// then
		ServiceException exception = assertThrows(ServiceException.class, () -> {
			keywordMetricHourlyService.findHourlyMetrics();
		});
		assertThat(exception.getCode()).isEqualTo("404");
		assertThat(exception.getMessage()).isEqualTo("키워드를 불러오지 못했습니다.");
	}

	@Test
	@DisplayName("신규성 점수 평가 - 낮은 변동성")
	void scoreNoveltyEvaluation_LowVariation() throws Exception {
		// given
		Long keywordId = 1L;
		Keyword keyword = Keyword.builder().id(keywordId).text("테스트 키워드").build();
		KeywordMetricHourly currentMetric = KeywordMetricHourly.builder()
			.id(KeywordMetricHourlyId.builder()
				.keywordId(keywordId)
				.bucketAt(LocalDateTime.now(ZoneId.of("UTC")))
				.platform(Platform.GOOGLE_TREND)
				.build())
			.keyword(keyword)
			.rankDelta(50.0)
			.weightedNovelty(3.0)
			.noPostStreak(1)
			.volume(100)
			.noveltyRatio(0.5)
			.build();
		List<Long> postableIds = new ArrayList<>();
		ArgumentCaptor<KeywordMetricHourly> metricCaptor = ArgumentCaptor.forClass(KeywordMetricHourly.class);
		when(keywordMetricHourlyRepository.save(metricCaptor.capture())).thenReturn(currentMetric);

		// when
		Method scoreNoveltyEvaluationMethod = KeywordMetricHourlyService.class.getDeclaredMethod(
			"scoreNoveltyEvaluation", KeywordMetricHourly.class, List.class);
		scoreNoveltyEvaluationMethod.setAccessible(true);
		boolean lowVariationResult = (boolean)scoreNoveltyEvaluationMethod.invoke(keywordMetricHourlyService,
			currentMetric, postableIds);

		// then
		assertThat(lowVariationResult).isTrue();
		assertThat(postableIds).isEmpty();
		KeywordMetricHourly savedMetric = metricCaptor.getValue();
		assertThat(savedMetric.isLowVariation()).isTrue();
		assertThat(savedMetric.getNoPostStreak()).isEqualTo(2);
		assertThat(savedMetric.getScore()).isEqualTo(((int)(50.0 / 100) + (int)(3.0) + 1) * 10000 + 100);
	}

	@Test
	@DisplayName("신규성 점수 평가 - 높은 변동성")
	void scoreNoveltyEvaluation_HighVariation() throws Exception {
		// given
		Long keywordId = 1L;
		Keyword keyword = Keyword.builder().id(keywordId).text("테스트 키워드").build();
		KeywordMetricHourly currentMetric = KeywordMetricHourly.builder()
			.id(KeywordMetricHourlyId.builder()
				.keywordId(keywordId)
				.bucketAt(LocalDateTime.now(ZoneId.of("UTC")))
				.platform(Platform.GOOGLE_TREND)
				.build())
			.keyword(keyword)
			.rankDelta(500.0)
			.weightedNovelty(7.0)
			.noPostStreak(3)
			.volume(200)
			.noveltyRatio(0.8)
			.build();
		List<Long> postableIds = new ArrayList<>();
		ArgumentCaptor<KeywordMetricHourly> metricCaptor = ArgumentCaptor.forClass(KeywordMetricHourly.class);
		when(keywordMetricHourlyRepository.save(metricCaptor.capture())).thenReturn(currentMetric);

		// when
		Method scoreNoveltyEvaluationMethod = KeywordMetricHourlyService.class.getDeclaredMethod(
			"scoreNoveltyEvaluation", KeywordMetricHourly.class, List.class);
		scoreNoveltyEvaluationMethod.setAccessible(true);
		boolean lowVariationResult = (boolean)scoreNoveltyEvaluationMethod.invoke(keywordMetricHourlyService,
			currentMetric, postableIds);

		// then
		assertThat(lowVariationResult).isFalse();
		assertThat(postableIds).containsExactly(keywordId);
		KeywordMetricHourly savedMetric = metricCaptor.getValue();
		assertThat(savedMetric.isLowVariation()).isFalse();
		assertThat(savedMetric.getNoPostStreak()).isZero();
		assertThat(savedMetric.getScore()).isEqualTo(((int)(500.0 / 100) + (int)(7.0) + 3) * 10000 + 200);
	}

	@Test
	@DisplayName("신규성 평가 - 데이터 부족")
	void evaluateNovelty_NoSufficientData() {
		// given
		// List가 하나밖에 없을 때 = 신규 작성일 경우
		Long keywordId = 1L;
		when(keywordMetricHourlyRepository.findById_KeywordIdOrderById_BucketAtDesc(keywordId)).thenReturn(
			List.of(KeywordMetricHourly.builder().build()));

		// when
		NoveltyStatsDto result = keywordMetricHourlyService.evaluateNovelty(List.of(keywordId));

		// then
		assertThat(result.lowVariationCount()).isZero();
		assertThat(result.postableIds()).containsExactly(keywordId);
		verify(keywordMetricHourlyRepository, never()).save(any());
	}

	@Test
	@DisplayName("신규성 점수 계산")
	void calculateNoveltyScoreTest() throws Exception {
		// given
		KeywordMetricHourly metric = KeywordMetricHourly.builder()
			.rankDelta(250.0)
			.weightedNovelty(5.5)
			.noPostStreak(2)
			.build();

		// when
		Method calculateNoveltyScoreMethod = KeywordMetricHourlyService.class.getDeclaredMethod("calculateNoveltyScore",
			KeywordMetricHourly.class);
		calculateNoveltyScoreMethod.setAccessible(true);
		int score = (int)calculateNoveltyScoreMethod.invoke(keywordMetricHourlyService, metric);

		// then
		assertThat(score).isEqualTo((int)(250.0 / 100) + (int)(5.5) + 2); // 2 + 5 + 2 = 9
	}

	@Test
	@DisplayName("키워드 메트릭 업데이트")
	void updateKeywordMetricTest() throws Exception {
		// given
		KeywordMetricHourlyId id = KeywordMetricHourlyId.builder()
			.keywordId(1L)
			.bucketAt(LocalDateTime.now(ZoneId.of("UTC")))
			.platform(Platform.GOOGLE_TREND)
			.build();
		Keyword keyword = Keyword.builder().id(1L).text("테스트 키워드").build();
		KeywordMetricHourly currentMetric = KeywordMetricHourly.builder()
			.id(id)
			.keyword(keyword)
			.post(null)
			.volume(150)
			.score(0)
			.rankDelta(100.0)
			.weightedNovelty(8.0)
			.noPostStreak(0)
			.noveltyRatio(0.9)
			.lowVariation(false)
			.build();
		int noveltyScore = 10;
		boolean lowVariation = true;
		int noPostStreak = 1;

		ArgumentCaptor<KeywordMetricHourly> metricCaptor = ArgumentCaptor.forClass(KeywordMetricHourly.class);
		when(keywordMetricHourlyRepository.save(metricCaptor.capture())).thenReturn(currentMetric);

		// when
		Method updateKeywordMetricMethod = KeywordMetricHourlyService.class.getDeclaredMethod("updateKeywordMetric",
			KeywordMetricHourly.class, int.class, boolean.class, int.class);
		updateKeywordMetricMethod.setAccessible(true);
		updateKeywordMetricMethod.invoke(keywordMetricHourlyService, currentMetric, noveltyScore, lowVariation,
			noPostStreak);

		// then
		verify(keywordMetricHourlyRepository, times(1)).save(any(KeywordMetricHourly.class));
		KeywordMetricHourly savedMetric = metricCaptor.getValue();
		assertThat(savedMetric.getScore()).isEqualTo((noveltyScore * 10000) + 150);
		assertThat(savedMetric.isLowVariation()).isTrue();
		assertThat(savedMetric.getNoPostStreak()).isEqualTo(1);
	}
}
