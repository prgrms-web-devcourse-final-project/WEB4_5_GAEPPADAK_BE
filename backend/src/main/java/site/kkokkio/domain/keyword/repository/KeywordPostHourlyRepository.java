package site.kkokkio.domain.keyword.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.keyword.entity.KeywordPostHourly;
import site.kkokkio.domain.keyword.entity.KeywordPostHourlyId;

@Repository
public interface KeywordPostHourlyRepository extends JpaRepository<KeywordPostHourly, KeywordPostHourlyId> {
	Optional<KeywordPostHourly> findById_KeywordIdAndId_BucketAt(Long keywordId, LocalDateTime bucketAt);
}
