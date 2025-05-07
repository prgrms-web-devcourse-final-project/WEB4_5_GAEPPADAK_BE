package site.kkokkio.domain.keyword.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.keyword.entity.Keyword;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {
	Optional<Keyword> findKeywordByText(String text);
}
