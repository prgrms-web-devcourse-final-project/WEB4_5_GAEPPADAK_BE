package site.kkokkio.domain.keyword.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;

@Repository
public interface KeywordMetricHourlyRepository extends JpaRepository<KeywordMetricHourly, KeywordMetricHourlyId> {
	List<KeywordMetricHourly> findById_BucketAtBetween(LocalDateTime start, LocalDateTime end);
	List<KeywordMetricHourly> findTop10ByOrderByCreatedAtDesc();
	Optional<KeywordMetricHourly> findTopByIdKeywordIdOrderByIdBucketAtDesc(Long keywordId);
	List<KeywordMetricHourly> findTop10ById_BucketAtOrderByScoreDesc(LocalDateTime bucketAt);
}
