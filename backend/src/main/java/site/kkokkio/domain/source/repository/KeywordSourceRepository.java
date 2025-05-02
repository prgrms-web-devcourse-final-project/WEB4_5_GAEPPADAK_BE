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
public interface KeywordSourceRepository extends JpaRepository<KeywordSource, Long>, KeywordSourceRepositoryCustom {
    /**
     * 특정 키워드에 연결된 Source 리스트를 최신 발행순으로 조회
     */
	@Query(
		value = "SELECT ks.source FROM KeywordSource ks " +
				"WHERE ks.keyword.id = :keywordId " +
				"ORDER BY ks.source.publishedAt DESC",
		countQuery = "SELECT COUNT(ks) FROM KeywordSource ks " +
					 "WHERE ks.keyword.id = :keywordId"
	)
	Page<Source> findSourcesByKeywordId(@Param("keywordId") Long keywordId, Pageable pageable);
}
