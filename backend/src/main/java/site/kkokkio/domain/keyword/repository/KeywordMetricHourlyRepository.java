package site.kkokkio.domain.keyword.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;

@Repository
public interface KeywordMetricHourlyRepository extends JpaRepository<KeywordMetricHourly, Long> {
	List<KeywordMetricHourly> findById_BucketAtBetween(LocalDateTime start, LocalDateTime end);
	List<KeywordMetricHourly> findTop10ByOrderByCreatedAtDesc();
}
