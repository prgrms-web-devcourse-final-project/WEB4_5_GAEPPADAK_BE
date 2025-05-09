package site.kkokkio.domain.keyword.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
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

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.global.enums.Platform;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTrendsRssService {

	private final KeywordService keywordService;
	private final KeywordMetricHourlyService keywordMetricHourlyService;

	@Value("${google.trends.rss.url}")
	private String googleTrendsRssUrl;

	@Value("${google.trends.rss.namespace}")
	private String namespaceUrl;

	@Value("${mock.enabled}")
	private boolean mockEnabled;

	@Value("classpath:mock/${mock.keyword-file}")
	private Resource mockKeywordsResource;

	private List<String> mockKeywords = List.of();
	private final Random random = new Random();

	/**
	 * Google Trends RSS를 호출해 실시간 키워드 목록을 수집하고,
	 * 각 키워드를 keyword + keyword_metric_hourly 테이블에 저장합니다.
	 *
	 * @return 수집된 키워드 텍스트 리스트
	 */
	@Transactional
	public List<Keyword> getTrendingKeywordsFromRss() {
		if (mockEnabled) {
			return generateMockKeywords();
		}
		List<Keyword> trendingKeywords = new ArrayList<>();
		try {
			URL feedUrl = new URL(googleTrendsRssUrl);
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(feedUrl));

			for (SyndEntry entry : feed.getEntries()) {
				saveKeywordMetric(entry.getTitle(), parseApproxTraffic(entry), trendingKeywords);
			}
		} catch (Exception e) {
			log.error("Failed to fetch Google Trends RSS", e);
		}
		return trendingKeywords;
	}

	private int parseApproxTraffic(SyndEntry entry) {
		String approxTraffic = entry.getForeignMarkup().stream()
			.filter(el -> "approx_traffic".equals(el.getName())
				&& namespaceUrl.equals(el.getNamespaceURI()))
			.map(org.jdom2.Element::getText)
			.findFirst()
			.orElse("0");
		if (approxTraffic.endsWith("+")) {
			return Integer.parseInt(approxTraffic.substring(0, approxTraffic.length() - 1));
		}
		try {
			return Integer.parseInt(approxTraffic);
		} catch (NumberFormatException e) {
			// 변환 실패 시 기본값 또는 에러 처리
			return 0; // 또는 다른 적절한 기본값
		}
	}

	private void saveKeywordMetric(String text, int volume, List<Keyword> trendingKeywords) {
		Keyword keyword = keywordService.createKeyword(
			Keyword.builder().text(text).build()
		);
		trendingKeywords.add(keyword);

		LocalDateTime bucketAt = LocalDateTime.now()
			.withMinute(0).withSecond(0).withNano(0);

		KeywordMetricHourlyId id = KeywordMetricHourlyId.builder()
			.keywordId(keyword.getId())
			.bucketAt(bucketAt)
			.platform(Platform.GOOGLE_TREND)
			.build();

		KeywordMetricHourly metric = KeywordMetricHourly.builder()
			.id(id)
			.keyword(keyword)
			.volume(volume)
			.score(volume)
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
			saveKeywordMetric(text, volume, trendingKeywords);
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