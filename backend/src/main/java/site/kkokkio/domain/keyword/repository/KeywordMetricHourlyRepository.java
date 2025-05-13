package site.kkokkio.domain.keyword.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;

@Repository
public interface KeywordMetricHourlyRepository extends JpaRepository<KeywordMetricHourly, KeywordMetricHourlyId> {
	@Query(value = """
		SELECT kmh.*
		FROM keyword_metric_hourly kmh
		WHERE kmh.bucket_at = (
		  SELECT MAX(bucket_at)
		  FROM keyword_metric_hourly
		  WHERE bucket_at <= :now
		)
		ORDER BY kmh.score DESC
		LIMIT 10
		""",
		nativeQuery = true)
	List<KeywordMetricHourly> findTop10HourlyMetricsClosestToNowNative(@Param("now") LocalDateTime now);
}
