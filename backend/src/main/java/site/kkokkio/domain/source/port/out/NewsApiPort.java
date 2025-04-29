package site.kkokkio.domain.source.port.out;

import java.util.List;

import reactor.core.publisher.Mono;
import site.kkokkio.domain.source.dto.NewsDto;

/**
 * 외부 뉴스 검색 API 호출용 Port
 */
public interface NewsApiPort {
    /**
     * fetchNews (비동기)
     * @param keyword 검색어
     * @param display 한 번에 표시할 검색 결과 개수(기본값: 10, 최댓값: 100)
     * @param start 검색 시작 위치(기본값: 1, 최댓값: 1000)
     * @param sort 검색 결과 정렬(sim: 정확도 내림차순 - 기본값, date: 날짜 내림차순)
     * @return list of NewsDto
     */
    Mono<List<NewsDto>> fetchNews(String keyword,
                            Integer display,
                            Integer start,
                            String sort);
}
