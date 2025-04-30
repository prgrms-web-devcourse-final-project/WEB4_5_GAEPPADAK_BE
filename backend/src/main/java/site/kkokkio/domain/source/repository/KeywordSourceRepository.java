package site.kkokkio.domain.source.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.source.entity.KeywordSource;
import site.kkokkio.domain.source.entity.Source;

@Repository
public interface KeywordSourceRepository extends JpaRepository<KeywordSource, Long> {
    /**
     * 특정 키워드에 연결된 Source 리스트 조회
     */
	@Query(
		value = "SELECT ks.source FROM KeywordSource ks " +
				"JOIN ks.keyword k " +
				"WHERE k.id = :keywordId",
		countQuery = "SELECT COUNT(ks) FROM KeywordSource ks " +
					 "JOIN ks.keyword k " +
					 "WHERE k.id = :keywordId"
	)
	Page<Source> findSourcesByKeywordId(@Param("keywordId") Long keywordId, Pageable pageable);
}
