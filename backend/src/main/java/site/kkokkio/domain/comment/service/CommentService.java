package site.kkokkio.domain.comment.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.domain.comment.dto.ReportedCommentSummary;
import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.comment.entity.CommentLike;
import site.kkokkio.domain.comment.entity.CommentReport;
import site.kkokkio.domain.comment.repository.CommentLikeRepository;
import site.kkokkio.domain.comment.repository.CommentReportRepository;
import site.kkokkio.domain.comment.repository.CommentRepository;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.exception.ServiceException;

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
	 * @param reason 신고 사유
	 */
	@Transactional
	public void reportComment(Long commentId, Member reporter, ReportReason reason) {

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
			.reason(reason)
			.build();

		// 6. 신고 정보 저장
		commentReportRepository.save(commentReport);

		// 7. 댓글의 신고 카운트 증가 및 저장
		comment.increaseReportCount();
		commentRepository.save(comment);
	}

	/**
	 * 관리자용 신고된 댓글 목록을 페이징, 정렬, 검색하여 조회합니다.
	 * @param pageable 페이징 및 정렬 정보
	 * @param searchTarget 검색 대상 필드
	 * @param searchValue 검색어
	 * @return 페이징된 ReportedCommentSummary 목록
	 */
	@Transactional(readOnly = true)
	public Page<ReportedCommentSummary> getReportedCommentsList
	(Pageable pageable, String searchTarget, String searchValue) {

		// 1. 정렬 옵션 검증 및 매핑
		Sort apiSort = pageable.getSort();
		Sort repositorySort = Sort.unsorted();

		// 정렬 속성 및 기본 정렬 방향 정의
		List<String> sortProperties = Arrays.asList("reportedAt", "reportCount");
		Sort.Direction defaultDirection = Sort.Direction.DESC;

		// Pageable의 Sort 객체 순회
		for (Sort.Order order : apiSort) {
			String property = order.getProperty();

			// 정렬 속성 이름이 허용된 목록에 있는지 확인
			if (!sortProperties.contains(property)) {
				// 허용되지 않은 정렬 속성이면 오류 발생
				throw new ServiceException("400", "부적절한 정렬 옵션입니다.");
			}

			// 정렬 속성 이름을 CommentRepositoryRepository 쿼리의 별칭과 연결
			repositorySort = repositorySort.and(Sort.by(order.getDirection(), property));
		}

		// 만약 Pageable에 정렬 정보가 전혀 없었다면 기본 정렬 적용
		if (repositorySort.isEmpty()) {
			repositorySort = Sort.by(defaultDirection, "reportedAt");
		}

		// Pageable 객체 재생성 (원본 Pageable의 다른 정보(page, size)를 유지하고 정렬만 대체
		Pageable repositoryPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), repositorySort);

		// 2. 검색 조건 매핑
		String searchNickname = null;
		String searchPostTitle = null;
		String searchCommentBody = null;
		String searchReportReason = null;

		// 검색 대상과 검색어가 모두 존재하고, 검색어가 공백만으로 이루어지지 않았다면 매핑 로직 실행
		if (searchTarget != null && searchValue != null && !searchValue.trim().isEmpty()) {
			String trimmedSearchTarget = searchTarget.trim().toLowerCase();
			String trimmedSearchValue = searchValue.trim();

			// 검색 대상 문자열을 Repository 메서드의 인자로 매핑
			switch (trimmedSearchTarget) {
				case "nickname" -> searchNickname = trimmedSearchValue;
				case "post title" -> searchPostTitle = trimmedSearchValue;
				case "comment body" -> searchCommentBody = trimmedSearchValue;
				case "report reason" -> searchReportReason = trimmedSearchValue;
				default -> throw new ServiceException("400", "부적절한 검색 옵션입니다.");
			}
		}

		// 3. ReportedCommentRepository 메서드 호출
		// 매핑된 검색 인자들과 정렬/페이징 정보(repositoryPageable)를 넘겨 호출
		Page<ReportedCommentSummary> reportedCommentPage =
			commentReportRepository.findReportedCommentSummary(
				searchNickname,
				searchPostTitle,
				searchCommentBody,
				searchReportReason,
				repositoryPageable
			);

		// 4. 결과 반환
		return reportedCommentPage;
	}

	/**
	 * 관리자용 신고된 댓글들을 소프트 삭제(숨김) 처리합니다.
	 * @param commentIds 숨길 댓글 ID 목록
	 */
	@Transactional
	public void hideReportedComment(List<Long> commentIds) {

		for (Long commentId : commentIds) {

			// 1. 댓글 조회 (소프트 삭제 여부와 상관없이 일단 존재하면 가져옴)
			Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글이 포함되어 있습니다."));

			// 2. 댓글을 숨김 처리
			comment.softDelete();

			// 3. 변경사항 저장
			commentRepository.save(comment);
		}
	}

	/**
	 * 관리자용 신고된 댓글들의 신고를 거부(삭제) 처리합니다.
	 * @param commentIds 신고를 거부할 댓글 ID 목록
	 */
	@Transactional
	public void rejectReportedComment(List<Long> commentIds) {

		// 1. 요청된 모든 댓글 ID에 해당하는 Comment 엔티티 조회
		List<Comment> comments = commentRepository.findAllById(commentIds);

		// 2. 조회된 댓글 개수와 요청된 ID 개수를 비교하여, 누락된 댓글(존재하지 않는 댓글)이 있는지 확인
		if (comments.size() != commentIds.size()) {
			throw new ServiceException("404", "존재하지 않는 댓글이 포함되어 있습니다.");
		}

		// 3. 요청된 댓글 ID들에 해당하는 모든 신고 엔티티 삭제
		commentReportRepository.deleteAllByCommentIdIn(commentIds);
	}
}
