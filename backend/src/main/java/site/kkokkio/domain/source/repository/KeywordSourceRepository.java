package site.kkokkio.domain.source.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.source.entity.KeywordSource;

@Repository
public interface KeywordSourceRepository extends JpaRepository<KeywordSource, Long>, KeywordSourceRepositoryCustom {
	/**
     * 키워드 목록에 연결된 Source 목록을 최신 발행순으로 조회
     */
	@Query(value = """
		SELECT ks.*
		FROM (
			SELECT ks.*, ROW_NUMBER() OVER (PARTITION BY ks.keyword_id ORDER BY s.published_at DESC) AS rn
			FROM keyword_source ks
			JOIN source s ON ks.fingerprint = s.fingerprint
			WHERE ks.keyword_id IN (:keywordIds)
		) ks
		WHERE ks.rn <= :limit
		""", nativeQuery = true)
	List<KeywordSource> findTopSourcesByKeywordIdsLimited(
		@Param("keywordIds") List<Long> keywordIds,
		@Param("limit") int limit
	);
}
