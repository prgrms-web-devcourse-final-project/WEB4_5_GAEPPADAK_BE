package site.kkokkio.domain.keyword.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.port.out.TrendsPort;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.infra.google.trends.dto.KeywordInfo;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendsService {
	private final TrendsPort trendsAdapter; // TrendsPort 인터페이스 주입

	private final KeywordService keywordService;
	private final KeywordMetricHourlyService keywordMetricHourlyService;
	private final KeywordMetricHourlyRepository keywordMetricHourlyRepository;

	@Value("${trend.platform}")
	private Platform platform;

	@Transactional
	public List<Keyword> getTrendingKeywordsFromRss() {
		List<Keyword> trendingKeywords = new ArrayList<>();
		List<KeywordInfo> trendingKeywordsInfo = trendsAdapter.fetchTrendingKeywords();

		LocalDateTime bucketAt = LocalDateTime.now()
			.withMinute(0).withSecond(0).withNano(0);
		for (KeywordInfo keywordInfo : trendingKeywordsInfo) {
			saveKeywordMetric(keywordInfo, trendingKeywords, bucketAt);
		}
		return trendingKeywords;
	}

	public void saveKeywordMetric(KeywordInfo keywordInfo, List<Keyword> trendingKeywords, LocalDateTime bucketAt) {
		Keyword keyword = keywordService.createKeyword(
			Keyword.builder().text(keywordInfo.getText()).build()
		);
		trendingKeywords.add(keyword);

		KeywordMetricHourlyId id = KeywordMetricHourlyId.builder()
			.keywordId(keyword.getId())
			.bucketAt(bucketAt)
			.platform(platform)
			.build();

		// 이전 시간의 메트릭 조회
		KeywordMetricHourly previousMetric = keywordMetricHourlyRepository
			.findTop1ById_KeywordIdAndId_BucketAtLessThanOrderById_BucketAtDesc(keyword.getId(), bucketAt).orElse(null);

		double rankDelta = 0.0;
		double noveltyRatio = 1.0;
		int noPostStreak = 0;
		if (previousMetric != null) {
			// RankDelta를 이전 대비 volume 변화량으로 계산
			rankDelta = keywordInfo.getVolume() - previousMetric.getVolume();
			noPostStreak = previousMetric.getNoPostStreak();

			// 이전 시간의 메트릭과의 시간 간격이 클수록 높은 신규성
			long hoursDifference = java.time.Duration.between(previousMetric.getId().getBucketAt(), bucketAt).toHours();
			if (hoursDifference > 24)
				noveltyRatio = 1.0;
			else if (hoursDifference > 12)
				noveltyRatio = 0.8;
			else if (hoursDifference > 6)
				noveltyRatio = 0.6;
			else
				noveltyRatio = 0.3;
		}

		int score = (((int)(noveltyRatio * 10) + noPostStreak) * 1000) + keywordInfo.getVolume();

		KeywordMetricHourly metric = KeywordMetricHourly.builder()
			.id(id)
			.keyword(keyword)
			.volume(keywordInfo.getVolume())
			.score(score)
			.rankDelta(rankDelta)
			.noPostStreak(noPostStreak)
			.noveltyRatio(noveltyRatio)
			.weightedNovelty(noveltyRatio * 10)
			.build();

		keywordMetricHourlyService.createKeywordMetricHourly(metric);
	}
}
