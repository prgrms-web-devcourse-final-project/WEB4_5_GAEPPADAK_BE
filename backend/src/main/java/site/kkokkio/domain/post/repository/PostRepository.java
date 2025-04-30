package site.kkokkio.domain.post.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import site.kkokkio.domain.post.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
	@Query(value = """
            SELECT p
            FROM Post p
            JOIN PostKeyword pk ON p.id = pk.post.id
            JOIN Keyword k ON pk.keyword.id = k.id
            WHERE k.text = :keywordText
            """,
		countQuery = """
                    SELECT COUNT(p)
                    FROM Post p
                    JOIN PostKeyword pk ON p.id = pk.post.id
                    JOIN Keyword k ON pk.keyword.id = k.id
                    WHERE k.text = :keywordText
                    """)
	Page<Post> findPostsByKeywordText(@Param("keywordText") String keywordText, Pageable pageable);
}
