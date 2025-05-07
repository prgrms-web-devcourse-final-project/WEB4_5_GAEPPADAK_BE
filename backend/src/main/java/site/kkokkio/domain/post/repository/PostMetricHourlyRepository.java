package site.kkokkio.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.post.entity.PostMetricHourly;

@Repository
public interface PostMetricHourlyRepository extends JpaRepository<PostMetricHourly, Long> {
}
