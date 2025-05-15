package site.kkokkio.domain.keyword.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.port.out.TrendsPort;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.infra.google.trends.dto.KeywordInfo;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendsService {
	private final TrendsPort trendsAdapter; // TrendsPort 인터페이스 주입

	private final KeywordService keywordService;
	private final KeywordMetricHourlyService keywordMetricHourlyService;

	private Platform platform = Platform.GOOGLE_TREND;

	@Value("${mock.enabled}")
	private boolean mockEnabled;

	@Value("classpath:mock/${mock.keyword-file}")
	private Resource mockKeywordsResource;

	private List<String> mockKeywords = List.of();
	private final Random random = new Random();

	@Transactional
	public List<Keyword> getTrendingKeywordsFromRss() {
		if (mockEnabled) {
			return generateMockKeywords();
		}
		List<Keyword> trendingKeywords = new ArrayList<>();
		List<KeywordInfo> trendingKeywordsInfo = trendsAdapter.fetchTrendingKeywords();
		for(KeywordInfo keywordInfo : trendingKeywordsInfo) {
			saveKeywordMetric(keywordInfo, trendingKeywords);
		}
		return trendingKeywords;
	}

	public void saveKeywordMetric(KeywordInfo keywordInfo, List<Keyword> trendingKeywords) {
		Keyword keyword = keywordService.createKeyword(
			Keyword.builder().text(keywordInfo.getText()).build()
		);
		trendingKeywords.add(keyword);

		LocalDateTime bucketAt = LocalDateTime.now()
			.withMinute(0).withSecond(0).withNano(0);

		KeywordMetricHourlyId id = KeywordMetricHourlyId.builder()
			.keywordId(keyword.getId())
			.bucketAt(bucketAt)
			.platform(platform)
			.build();

		KeywordMetricHourly metric = KeywordMetricHourly.builder()
			.id(id)
			.keyword(keyword)
			.volume(keywordInfo.getVolume())
			.score(keywordInfo.getVolume())
			.build();

		keywordMetricHourlyService.createKeywordMetricHourly(metric);
	}

	private List<Keyword> generateMockKeywords() {
		List<Keyword> trendingKeywords = new ArrayList<>();
		int count = 10;
		for (int i = 0; i < count; i++) {
			String text = mockKeywords.isEmpty()
				? "테스트키워드" + i
				: mockKeywords.get(random.nextInt(mockKeywords.size()));
			int volume = random.nextInt(1001); // 0~1000 범위
			saveKeywordMetric(KeywordInfo.builder().text(text).volume(volume).build(), trendingKeywords);
		}
		return trendingKeywords;
	}

	@PostConstruct
	private void loadMockKeywords() {
		if (!mockEnabled) {
			return;
		}
		if (!mockKeywordsResource.exists()) {
			log.error("[mock] 키워드 리소스를 찾을 수 없습니다: {}", mockKeywordsResource);
			return;
		}
		try (InputStream is = mockKeywordsResource.getInputStream();
			 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			mockKeywords = reader.lines()
				.map(String::trim)
				.filter(line -> !line.isEmpty())
				.collect(Collectors.toList());
			log.info("[mock] Loaded {} keywords from {}", mockKeywords.size(), mockKeywordsResource.getFilename());
		} catch (IOException e) {
			log.error("[mock] Failed to load mock keywords", e);
		}
	}
}
