package site.kkokkio.domain.source.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.source.dto.TopSourceItemDto;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.global.enums.Platform;

@Repository
public interface PostSourceRepository extends JpaRepository<PostSource, Long>, PostSourceRepositoryCustom {
	@Query("""
		SELECT ps
		FROM PostSource ps
		JOIN FETCH ps.source
		WHERE ps.post.id = :postId
		  AND ps.source.platform = :platform
		ORDER BY ps.source.publishedAt DESC
		""")
	List<PostSource> findAllWithSourceByPostIdAndPlatform(
		@Param("postId") Long postId,
		@Param("platform") Platform platform,
		Pageable pageable
	);

	/**
	 * 주어진 인기 키워드 ID 목록과 플랫폼에 해당하는 Source 엔티티들을
	 * 관련 관계(keyword_metric_hourly -> post -> post_source -> source)를 따라 조회하고
	 * 페이지네이션 및 인기순(키워드 점수 등) 정렬을 적용하여 반환합니다.
	 * @param topKeywordIds 인기 키워드의 ID 목록.
	 * @param platform 조회할 플랫폼 (YOUTUBE 또는 NAVER_NEWS).
	 * @param pageable 페이지네이션 및 정렬 정보. Service에서 전달한 정렬 정보가 쿼리에 자동 반영됩니다.
	 * @return 페이지네이션된 Source 엔티티 목록.
	 */
	@Query(
	value = """
            SELECT DISTINCT s
            FROM KeywordMetricHourly kmh
            LEFT JOIN kmh.post p
            JOIN PostSource ps ON ps.post = p
            JOIN ps.source s
            WHERE kmh.id.keywordId IN (:topKeywordIds)
            AND s.platform = :platform
            ORDER BY s.publishedAt DESC
            """,
	countQuery = """
			SELECT COUNT(DISTINCT s.fingerprint)
			FROM KeywordMetricHourly kmh
			LEFT JOIN kmh.post p
			JOIN PostSource ps ON ps.post = p
			JOIN ps.source s
			WHERE kmh.id.keywordId IN (:topKeywordIds)
			AND s.platform = :platform
			""")
	Page<Source> findSourcesByTopKeywordIdsAndPlatform(
			@Param("topKeywordIds") List<Long> topKeywordIds,
			@Param("platform") Platform platform,
			Pageable pageable
	);

	/**
	 * 실시간 인기 키워드 ID와 플랫폼 기반으로, 출처(Source) 정보를 score 기준 내림차순 정렬하여 DTO로 조회.
	 * DISTINCT 문제 해결을 위해 SELECT new 사용.
	 */
	@Query("""
            SELECT new site.kkokkio.domain.source.dto.TopSourceItemDto(
            	s.fingerprint,
                s.normalizedUrl,
                s.title,
                s.description,
                s.thumbnailUrl,
                s.publishedAt,
                s.platform,
                kmh.score
                )
            FROM KeywordMetricHourly kmh
            LEFT JOIN kmh.post p
            JOIN PostSource ps
            ON ps.post = p
            JOIN ps.source s
            WHERE kmh.id.keywordId
            IN (:topKeywordIds)
                AND s.platform = :platform
            ORDER BY kmh.score DESC
            """)
	Page<TopSourceItemDto> findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
			@Param("topKeywordIds") List<Long> topKeywordIds,
			@Param("platform") Platform platform,
			Pageable pageable
	);
}
