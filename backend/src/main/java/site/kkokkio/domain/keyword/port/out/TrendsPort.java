package site.kkokkio.domain.keyword.port.out;

import java.util.List;

import site.kkokkio.infra.google.trends.dto.KeywordInfo;

public interface TrendsPort {
	/**
	 * Google Trends RSS를 호출하여 실시간 키워드 목록을 가져옵니다.
	 *
	 * @return 수집된 키워드 텍스트 리스트
	 */
	List<KeywordInfo> fetchTrendingKeywords();
}
