package site.kkokkio.domain.keyword.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;

@Service
@RequiredArgsConstructor
public class KeywordMetricHourlyService {
	private final KeywordMetricHourlyRepository keywordMetricHourlyRepository;

	@Transactional
	public KeywordMetricHourly createKeywordMetricHourly(KeywordMetricHourly keywordMetricHourly) {
		return keywordMetricHourlyRepository.save(keywordMetricHourly);
	}
}
