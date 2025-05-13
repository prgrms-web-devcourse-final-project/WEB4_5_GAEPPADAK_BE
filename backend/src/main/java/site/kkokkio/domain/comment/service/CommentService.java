package site.kkokkio.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.domain.comment.dto.CommentReportRequestDto;
import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.comment.entity.CommentLike;
import site.kkokkio.domain.comment.repository.CommentLikeRepository;
import site.kkokkio.domain.comment.repository.CommentRepository;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.domain.report.entity.CommentReport;
import site.kkokkio.domain.report.repository.CommentReportRepository;
import site.kkokkio.global.exception.ServiceException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final CommentLikeRepository commentLikeRepository;
	private final CommentReportRepository commentReportRepository;

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

	@Transactional
	public CommentDto unlikeComment(Long commentId, Member member) {
		Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		if (comment.getMember().getId().equals(member.getId())) {
			throw new ServiceException("403", "본인 댓글은 좋아요 할 수 없습니다.");
		}

		if (commentLikeRepository.existsByComment(comment)) {
			commentLikeRepository.deleteByComment(comment);
			comment.decreaseLikeCount();
			commentRepository.save(comment);
		} else {
			throw new ServiceException("400", "이미 좋아요가 취소된 상태입니다.");
		}
		return CommentDto.from(comment);
	}

	/**
	 * 댓글 신고 기능
	 * @param commentId 신고 대상 댓글 ID
	 * @param reporter 신고하는 사용자 (인증된 사용자)
	 * @param request 신고 요청 DTO (신고 사유 포함)
	 */
	@Transactional
	public void reportComment(Long commentId, Member reporter, CommentReportRequestDto request) {

		// 1. 신고 대상 댓글 조회
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		// 2. 삭제된 댓글인지 확인
		if (comment.isDeleted()) {
			throw new ServiceException("400", "삭제된 댓글은 신고할 수 없습니다.");
		}

		// 3. 본인 댓글 신고 방지
		if (comment.getMember().getId().equals(reporter.getId())) {
			throw new ServiceException("403", "본인의 댓글은 신고할 수 없습니다.");
		}

		// 4. 중복 신고 방지
		boolean alreadyReported = commentReportRepository.existsByCommentAndReporter(comment, reporter);

		if (alreadyReported) {
			throw new ServiceException("400", "이미 신고한 댓글입니다.");
		}

		// 5. 신고 정보 생성
		CommentReport commentReport = CommentReport.builder()
				.comment(comment)
				.reporter(reporter)
				.reason(request.reason())
				.build();

		// 6. 신고 정보 저장
		commentReportRepository.save(commentReport);

		// 7. 댓글의 신고 카운트 증가 및 저장
		comment.increaseReportCount();
		commentRepository.save(comment);
	}
}
