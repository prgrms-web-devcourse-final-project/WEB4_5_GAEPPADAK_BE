package site.kkokkio.domain.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.controller.dto.CommentResponse;
import site.kkokkio.domain.comment.entity.Comment;
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

	public Page<CommentResponse> getCommentListByPostId(Long postId, Pageable pageable) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 포스트입니다."));

		return commentRepository.findAllByPostAndDeletedAtIsNull(post, pageable)
			.map(CommentResponse::from);
	}

	public CommentResponse createComment(Long postId, Member member, CommentCreateRequest request) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 포스트입니다."));

		Comment comment = Comment.builder()
			.post(post)
			.member(member)
			.body(request.body())
			.build();

		commentRepository.save(comment);

		return CommentResponse.from(comment);
	}

	public CommentResponse updateComment(Long commentId, Member member, CommentCreateRequest request) {
		Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		if (!comment.getMember().equals(member)) {
			throw new ServiceException("403", "본인 댓글만 수정할 수 있습니다.");
		}

		comment.updateBody(request.body());

		return CommentResponse.from(comment);
	}

	public void deleteCommentById(Long commentId, Member member) {
		Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		if (!comment.getMember().equals(member)) {
			throw new ServiceException("403", "본인 댓글만 삭제할 수 있습니다.");
		}

		comment.softDelete();
	}
}
