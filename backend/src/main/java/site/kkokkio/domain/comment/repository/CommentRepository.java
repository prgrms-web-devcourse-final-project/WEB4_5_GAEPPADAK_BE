package site.kkokkio.domain.comment.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.post.entity.Post;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
	Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

	@EntityGraph(attributePaths = {"member"})
	Page<Comment> findAllByPostAndDeletedAtIsNull(Post post, Pageable pageable);
}
