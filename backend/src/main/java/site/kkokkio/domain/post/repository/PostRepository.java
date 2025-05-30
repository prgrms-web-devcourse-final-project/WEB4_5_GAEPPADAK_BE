package site.kkokkio.domain.post.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import site.kkokkio.domain.post.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
	Optional<Post> findByIdAndDeletedAtIsNull(Long id);
}
