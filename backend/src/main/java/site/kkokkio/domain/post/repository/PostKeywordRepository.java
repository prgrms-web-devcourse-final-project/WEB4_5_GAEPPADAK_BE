package site.kkokkio.domain.post.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.post.entity.PostKeyword;

@Repository
public interface PostKeywordRepository extends JpaRepository<PostKeyword, Long> {
	Optional<PostKeyword> findByPost_Id(Long postId);
}
