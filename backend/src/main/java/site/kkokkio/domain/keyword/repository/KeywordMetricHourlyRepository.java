package site.kkokkio.domain.keyword.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;

@Repository
public interface KeywordMetricHourlyRepository extends JpaRepository<KeywordMetricHourly, Long> {

}
