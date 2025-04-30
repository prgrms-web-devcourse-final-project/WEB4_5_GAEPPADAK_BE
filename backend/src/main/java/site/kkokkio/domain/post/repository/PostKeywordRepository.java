package site.kkokkio.domain.post.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.post.entity.PostKeyword;

@Repository
public interface PostKeywordRepository extends JpaRepository<PostKeyword, Long> {

	@Query(value = """
           SELECT pk
           FROM PostKeyword pk
           JOIN FETCH pk.post p
           JOIN FETCH pk.keyword k
           WHERE k.text = :keywordText
           """,
		countQuery = """
                   SELECT COUNT(pk)
                   FROM PostKeyword pk
                   JOIN pk.keyword k ON pk.keyword.id = k.id
                   WHERE k.text = :keywordText
                   """)
	Page<PostKeyword> findByKeywordTextWithPostAndKeyword(@Param("keywordText") String keywordText, Pageable pageable);
	Optional<PostKeyword> findByPost_Id(Long postId);
}
