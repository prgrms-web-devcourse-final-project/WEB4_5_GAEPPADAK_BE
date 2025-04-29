package site.kkokkio.domain.source.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.global.enums.Platform;

@Repository
public interface SourceRepository extends JpaRepository<Source, String> {
	/**
	 * TODO: source-keyword 조회가 많아질 경우 정규화를 깨고 SourceKeyword 테이블 설계 필요
	 */
    @Query("""
        select s
        from Source s
		where s.platform = :platform
		and exists (
			select 1
			from PostSource ps
				join ps.post p
				join PostKeyword pk on pk.post = p
				join pk.keyword k
			where ps.source = s
			and k.text = :keyword
		)
		order by s.publishedAt desc
		""")
	List<Source> findLatest10ByPlatformAndKeyword(
        @Param("platform") Platform platform,
        @Param("keyword")  String   keyword,
        Pageable pageable);
}
