package site.kkokkio.domain.source.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.lettuce.core.dynamic.annotation.Param;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.global.enums.Platform;

@Repository
public interface PostSourceRepository extends JpaRepository<PostSource, Long> {
	@Query("""
		SELECT ps
		FROM PostSource ps
		JOIN FETCH ps.source
		WHERE ps.post.id = :postId
		  AND ps.source.platform = :platform
		ORDER BY ps.source.publishedAt DESC
		""")
	List<PostSource> findAllWithSourceByPostIdAndPlatform(
		@Param("postId") Long postId,
		@Param("platform") Platform platform,
		Pageable pageable
	);
}
