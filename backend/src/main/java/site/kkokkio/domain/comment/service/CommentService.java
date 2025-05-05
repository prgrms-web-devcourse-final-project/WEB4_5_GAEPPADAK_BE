package site.kkokkio.domain.comment.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.comment.entity.CommentLike;
import site.kkokkio.domain.comment.repository.CommentLikeRepository;
import site.kkokkio.domain.comment.repository.CommentRepository;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.global.exception.ServiceException;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final CommentLikeRepository commentLikeRepository;

	public Page<CommentDto> getCommentListByPostId(Long postId, Pageable pageable) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 포스트입니다."));

		return commentRepository.findAllByPostAndDeletedAtIsNull(post, pageable)
			.map(CommentDto::from);
	}

	@Transactional
	public CommentDto createComment(Long postId, Member member, CommentCreateRequest request) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 포스트입니다."));

		Comment comment = Comment.builder()
			.post(post)
			.member(member)
			.body(request.body())
			.build();

		return CommentDto.from(commentRepository.save(comment));
	}

	@Transactional
	public CommentDto updateComment(Long commentId, UUID memberId, CommentCreateRequest request) {
		Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		if (!comment.getMember().getId().equals(memberId)) {
			throw new ServiceException("403", "본인 댓글만 수정할 수 있습니다.");
		}

		comment.updateBody(request.body());
		commentRepository.save(comment);

		return CommentDto.from(comment);
	}

	@Transactional
	public void deleteCommentById(Long commentId, UUID memberId) {
		Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		if (!comment.getMember().getId().equals(memberId)) {
			throw new ServiceException("403", "본인 댓글만 삭제할 수 있습니다.");
		}

		comment.softDelete();
		commentRepository.save(comment);
	}

	@Transactional
	public CommentDto likeComment(Long commentId, Member member) {
		Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		if (comment.getMember().getId().equals(member.getId())) {
			throw new ServiceException("403", "본인 댓글은 좋아요 할 수 없습니다.");
		}

		if (commentLikeRepository.existsByComment(comment)) {
			throw new ServiceException("400", "이미 좋아요를 누른 댓글입니다.");
		}

		CommentLike commentLike = CommentLike.builder()
			.comment(comment)
			.member(member)
			.build();
		commentLikeRepository.save(commentLike);

		comment.increaseLikeCount();
		commentRepository.save(comment);

		return CommentDto.from(comment);
	}
}
