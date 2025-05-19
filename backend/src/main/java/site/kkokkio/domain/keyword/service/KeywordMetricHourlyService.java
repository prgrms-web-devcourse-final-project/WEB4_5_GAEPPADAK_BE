package site.kkokkio.domain.keyword.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyDto;
import site.kkokkio.domain.keyword.dto.NoveltyStatsDto;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.global.exception.ServiceException;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordMetricHourlyService {
	private final KeywordMetricHourlyRepository keywordMetricHourlyRepository;

	@Transactional
	public KeywordMetricHourly createKeywordMetricHourly(KeywordMetricHourly keywordMetricHourly) {
		return keywordMetricHourlyRepository.save(keywordMetricHourly);
	}

	// 인기 키워드 10개 조회
	@Transactional(readOnly = true)
	public List<KeywordMetricHourlyDto> findHourlyMetrics() {

		List<KeywordMetricHourly> metrics = keywordMetricHourlyRepository.findTop10HourlyMetricsClosestToNowNative(
			LocalDateTime.now()
		);
		if (metrics == null || metrics.isEmpty()) {
			throw new ServiceException("404", "키워드를 불러오지 못했습니다.");
		}
		List<KeywordMetricHourlyDto> responses = new ArrayList<>();
		for (KeywordMetricHourly metric : metrics) {
			Long postId = (metric.getPost() != null) ? metric.getPost().getId() : null;
			responses.add(new KeywordMetricHourlyDto(
				metric.getId().getKeywordId(),
				metric.getKeyword().getText(),
				metric.getId().getPlatform(),
				metric.getId().getBucketAt(),
				metric.getVolume(),
				metric.getScore(),
				metric.isLowVariation(),
				postId
			));
		}
		return responses;
	}

	@Transactional
	public NoveltyStatsDto evaluateNovelty(List<Long> topKeywordIds) {
		int lowVariationCount = 0;
		List<Long> postableIds = new ArrayList<>();

		for (Long keywordId : topKeywordIds) {
			int score = 0;
			boolean lowVariation = false;
			int noPostStreak = 0;
			List<KeywordMetricHourly> allMetric = keywordMetricHourlyRepository
				.findById_KeywordIdOrderById_BucketAtDesc(keywordId);

			if (allMetric.size() >= 2) {
				KeywordMetricHourly currentMetric = allMetric.getFirst();

				// RankDelta, WeightedNovelty, NoPostStreak 을 종합적으로 판단하여 점수 작성
				score = calculateNoveltyScore(currentMetric);

				// 낮은 변동성 판단, 종합 10점 미만일 시 신규성이 낮다고 판단되어 포스트 생성 제외
				if (score < 10) {
					log.info("낮은 변동성 : 생성하지 않을 포스트의 키워드 id {}", keywordId);
					lowVariation = true;
					noPostStreak = currentMetric.getNoPostStreak() + 1;
					lowVariationCount++;
				} else {
					log.info("높은 변동성 : 생성할 포스트의 키워드 id {}", keywordId);
					postableIds.add(keywordId);
				}

				// 최종 스코어 (신규성 스코어 우선 / 신규성 스코어가 같을 시 검색량 우선하도록 score * 10000 삽입)
				updateKeywordMetric(currentMetric, score, lowVariation, noPostStreak);
			} else {
				// (데이터가 없는 경우) 포스팅 대상으로 추가
				postableIds.add(keywordId);
			}
		}

		return new NoveltyStatsDto(lowVariationCount, postableIds);
	}

	private int calculateNoveltyScore(KeywordMetricHourly metric) {
		int score = 0;
		score += (int) (metric.getRankDelta() / 100);
		score += (int) (metric.getWeightedNovelty());
		score += metric.getNoPostStreak();
		return score;
	}

	private void updateKeywordMetric(KeywordMetricHourly currentMetric, int noveltyScore, boolean lowVariation, int noPostStreak) {
		KeywordMetricHourly updatedMetric = KeywordMetricHourly.builder()
			.id(currentMetric.getId())
			.keyword(currentMetric.getKeyword())
			.post(currentMetric.getPost())
			.volume(currentMetric.getVolume())
			.score((noveltyScore * 10000) + currentMetric.getVolume())
			.rankDelta(currentMetric.getRankDelta())
			.weightedNovelty(currentMetric.getWeightedNovelty())
			.noPostStreak(noPostStreak)
			.noveltyRatio(currentMetric.getNoveltyRatio())
			.lowVariation(lowVariation)
			.build();

		keywordMetricHourlyRepository.save(updatedMetric);
	}
}
