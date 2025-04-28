package site.kkokkio.domain.keyword.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.global.enums.Platform;

@Service
@RequiredArgsConstructor
public class GoogleTrendsRssService {

	private final KeywordService keywordService;
	private final KeywordMetricHourlyService keywordMetricHourlyService;
	private static final String GOOGLE_TRENDS_RSS_URL = "https://trends.google.co.kr/trending/rss?geo=KR";

	@Transactional
	public List<String> getTrendingKeywordsFromRss() {
		List<String> trendingKeywords = new ArrayList<>();
		try {
			URL feedUrl = new URL(GOOGLE_TRENDS_RSS_URL);
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(feedUrl));

			for (SyndEntry entry : feed.getEntries()) {
				// Keyword 생성 또는 조회
				Keyword keyword = keywordService.createKeyword(
					Keyword.builder()
						.text(entry.getTitle())
						.build()
				);

				String approxTraffic = "";
				List<org.jdom2.Element> foreignMarkups = entry.getForeignMarkup();
				for (org.jdom2.Element element : foreignMarkups) {
					if ("approx_traffic".equals(element.getName()) && "https://trends.google.com/trending/rss".equals(element.getNamespaceURI())) {
						approxTraffic = element.getText();
						break;
					}
				}

				int volume = parseApproxTraffic(approxTraffic);

				// KeywordMetricHourlyId 생성 및 Keyword 설정
				KeywordMetricHourlyId id = KeywordMetricHourlyId.builder()
					.keywordId(keyword.getId())
					.bucketAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
					.platform(Platform.GOOGLE_TREND)
					.build();

				KeywordMetricHourly keywordMetricHourly = KeywordMetricHourly.builder()
					.id(id) // 생성한 ID 설정
					.keyword(keyword)
					.volume(volume)
					.score(volume)
					.build();

				keywordMetricHourlyService.createKeywordMetricHourly(keywordMetricHourly);

				trendingKeywords.add(entry.getTitle());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return trendingKeywords;
	}

	private Integer parseApproxTraffic(String approxTraffic) {
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