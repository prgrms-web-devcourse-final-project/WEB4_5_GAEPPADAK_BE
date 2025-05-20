package site.kkokkio.infra.google.trends;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.keyword.port.out.TrendsPort;
import site.kkokkio.global.exception.ExternalApiException;
import site.kkokkio.infra.google.trends.dto.KeywordInfo;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTrendsAdapter implements TrendsPort {
	@Value("${mock.enabled}")
	private boolean mockEnabled;

	@Value("classpath:mock/${mock.keyword-file}")
	private Resource mockKeywordsResource;

	@Value("${google.trends.rss.url}")
	private String googleTrendsRssUrl;

	@Value("${google.trends.rss.namespace}")
	private String namespaceUrl;

	private List<String> mockKeywords = List.of();
	private final Random random = new Random();

	@Override
	public List<KeywordInfo> fetchTrendingKeywords() {
		if (mockEnabled) {
			return generateMockKeywords();
		}

		try (XmlReader xmlReader = new XmlReader(new URL(googleTrendsRssUrl))) {
			SyndFeed feed = new SyndFeedInput().build(xmlReader);
			return feed.getEntries().stream()
				.map(this::toKeywordInfo)
				.toList();
		} catch (Exception e) {
			throw new ExternalApiException("Failed to fetch Google Trends RSS", e);
		}
	}

	private KeywordInfo toKeywordInfo(SyndEntry entry) {
		return KeywordInfo.builder()
			.text(entry.getTitle())
			.volume(parseApproxTraffic(entry))
			.build();
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

	private List<KeywordInfo> generateMockKeywords() {
		List<KeywordInfo> trendingKeywordInfos = new ArrayList<>();
		int count = 10;
		for (int i = 0; i < count; i++) {
			String text = mockKeywords.isEmpty()
				? "테스트키워드" + i
				: mockKeywords.get(random.nextInt(mockKeywords.size()));
			int volume = random.nextInt(1001); // 0~1000 범위
			trendingKeywordInfos.add(KeywordInfo.builder().text(text).volume(volume).build());
		}
		return trendingKeywordInfos;
	}

	@PostConstruct
	private void loadMockKeywords() {
		if (!mockEnabled) {
			return;
		}
		if (!mockKeywordsResource.exists()) {
			log.error("[mock] 키워드 리소스를 찾을 수 없습니다: {}", mockKeywordsResource.getFilename());
			return;
		}
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(mockKeywordsResource.getInputStream(), StandardCharsets.UTF_8))) {

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
