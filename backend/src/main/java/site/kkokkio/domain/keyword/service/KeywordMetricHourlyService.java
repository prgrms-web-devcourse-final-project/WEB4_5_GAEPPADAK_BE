package site.kkokkio.domain.keyword.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.controller.dto.KeywordMetricHourlyResponse;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.global.exception.ServiceException;

@Service
@RequiredArgsConstructor
public class KeywordMetricHourlyService {
	private final KeywordMetricHourlyRepository keywordMetricHourlyRepository;

	@Transactional
	public KeywordMetricHourly createKeywordMetricHourly(KeywordMetricHourly keywordMetricHourly) {
		return keywordMetricHourlyRepository.save(keywordMetricHourly);
	}

	// 가장 최근의 인기 키워드 10개 조회
	@Transactional(readOnly = true)
	public List<KeywordMetricHourlyResponse> findHourlyMetrics() {
		List<KeywordMetricHourly> metrics = keywordMetricHourlyRepository.findTop10ByOrderByCreatedAtDesc();
		if(metrics == null || metrics.isEmpty()) {
			throw new ServiceException("400", "키워드를 불러오지 못했습니다.");
		}
		List<KeywordMetricHourlyResponse> responses = new ArrayList<>();
		for (KeywordMetricHourly metric : metrics) {
			responses.add(new KeywordMetricHourlyResponse(
				metric.getId().getKeywordId(),
				metric.getKeyword().getText(),
				metric.getId().getPlatform(),
				metric.getId().getBucketAt(),
				metric.getVolume(),
				metric.getScore()
			));
		}
		return responses;
	}
}
