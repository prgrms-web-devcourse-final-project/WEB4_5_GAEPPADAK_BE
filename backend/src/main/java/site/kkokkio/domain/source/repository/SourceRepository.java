package site.kkokkio.domain.source.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.source.entity.Source;

@Repository
public interface SourceRepository extends JpaRepository<Source, String>, SourceRepositoryCustom {
	@Query("""
		SELECT ps.source
		FROM PostSource ps
		WHERE ps.post.id IN :postIds
		ORDER BY ps.source.publishedAt DESC
		""")
	List<Source> findByPostIdsOrderByPublishedAtDesc(@Param("postIds") List<Long> postIds, Pageable pageable);
}
