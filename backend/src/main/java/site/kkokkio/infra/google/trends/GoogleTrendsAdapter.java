package site.kkokkio.infra.google.trends;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.keyword.port.out.TrendsPort;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.keyword.service.KeywordService;
import site.kkokkio.global.exception.ExternalApiException;
import site.kkokkio.infra.google.trends.dto.KeywordInfo;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTrendsAdapter implements TrendsPort {
	private final KeywordService keywordService;
	private final KeywordMetricHourlyService keywordMetricHourlyService;

	@Value("${google.trends.rss.url}")
	private String googleTrendsRssUrl;

	@Value("${google.trends.rss.namespace}")
	private String namespaceUrl;

	@Override
	public List<KeywordInfo> fetchTrendingKeywords() {
		List<KeywordInfo> trendingKeywordsInfo = new ArrayList<>();
		try {
			URL feedUrl = new URL(googleTrendsRssUrl);
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(feedUrl));

			for (SyndEntry entry : feed.getEntries()) {
				String text = entry.getTitle();
				int volume = parseApproxTraffic(entry);
				trendingKeywordsInfo.add(KeywordInfo.builder().text(text).volume(volume).build());
			}
		} catch (Exception e) {
			throw new ExternalApiException("Failed to fetch Google Trends RSS", e);
		}
		return trendingKeywordsInfo;
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
}
