package site.kkokkio.domain.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.comment.entity.CommentLike;
import site.kkokkio.domain.member.entity.Member;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
	Boolean existsByCommentAndMember(Comment comment, Member member);

	void deleteByCommentAndMember(Comment comment, Member member);
}
