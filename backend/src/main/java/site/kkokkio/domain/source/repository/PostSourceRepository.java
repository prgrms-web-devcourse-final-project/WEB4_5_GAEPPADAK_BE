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
	 * 실시간 인기 키워드 ID와 플랫폼 기반으로, 출처(Source) 정보를 score 기준 내림차순 정렬하여 DTO로 조회.
	 * DISTINCT 문제 해결을 위해 SELECT new 사용.
	 */

	@Query(value = """
		SELECT new site.kkokkio.domain.source.dto.TopSourceItemDto(
		    s.fingerprint,
		    s.normalizedUrl,
		    s.title,
		    s.description,
		    s.thumbnailUrl,
		    s.publishedAt,
		    s.platform,
		    MAX(kmh.score)
		)
		FROM KeywordMetricHourly kmh
		JOIN PostSource ps ON ps.post = kmh.post
		JOIN ps.source s
		WHERE kmh.post.id IN :postIds
		  AND s.platform = :platform
		GROUP BY
		    s.fingerprint,
		    s.normalizedUrl,
		    s.title,
		    s.description,
		    s.thumbnailUrl,
		    s.publishedAt,
		    s.platform
		ORDER BY MAX(kmh.score) DESC
		""",
		countQuery = """
			SELECT COUNT(DISTINCT s.fingerprint)
			FROM KeywordMetricHourly kmh
			JOIN PostSource ps      ON ps.post = kmh.post
			JOIN ps.source          s
			WHERE kmh.post.id   IN :postIds
			  AND s.platform      = :platform
			"""
	)
	Page<TopSourceItemDto> findTopSourcesByPostIdsAndPlatformOrderedByScore(
		@Param("postIds") List<Long> postIds,
		@Param("platform") Platform platform,
		Pageable pageable
	);
}
