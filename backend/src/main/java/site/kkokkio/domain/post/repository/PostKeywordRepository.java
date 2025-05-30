package site.kkokkio.domain.post.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.post.entity.PostKeyword;

@Repository
public interface PostKeywordRepository extends JpaRepository<PostKeyword, Long>, PostKeywordRepositoryCustom {

	@Query(value = """
		SELECT pk
		FROM PostKeyword pk
		JOIN FETCH pk.post p
		JOIN FETCH pk.keyword k
		WHERE k.text = :keywordText
			AND p.deletedAt IS NULL
		""",
		countQuery = """
			SELECT COUNT(pk)
			FROM PostKeyword pk
			JOIN pk.keyword k ON pk.keyword.id = k.id
			JOIN pk.post p ON pk.post.id = p.id
			WHERE k.text = :keywordText
				AND p.deletedAt IS NULL
			""")
	Page<PostKeyword> findByKeywordTextWithPostAndKeyword(@Param("keywordText") String keywordText, Pageable pageable);

	Optional<PostKeyword> findByPost_Id(Long postId);

	Optional<PostKeyword> findTopByKeywordIdOrderByPost_BucketAtDesc(Long keywordId);

	@Query("SELECT pk FROM PostKeyword pk " +
		"JOIN FETCH pk.keyword k " +
		"JOIN FETCH pk.post p " +
		"WHERE pk.post.id IN :postIds")
	List<PostKeyword> findAllByPostIdIn(@Param("postIds") List<Long> postIds);
}
