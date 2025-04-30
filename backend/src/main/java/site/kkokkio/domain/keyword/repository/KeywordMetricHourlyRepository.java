package site.kkokkio.domain.keyword.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;

@Repository
public interface KeywordMetricHourlyRepository extends JpaRepository<KeywordMetricHourly, KeywordMetricHourlyId> {
	List<KeywordMetricHourly> findTop10ByOrderByCreatedAtDesc();
	List<KeywordMetricHourly> findTop10ById_BucketAtOrderByScoreDesc(LocalDateTime bucketAt);
}
