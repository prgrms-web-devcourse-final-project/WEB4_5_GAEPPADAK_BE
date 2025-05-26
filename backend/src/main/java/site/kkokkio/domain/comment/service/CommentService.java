package site.kkokkio.domain.comment.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.controller.dto.CommentReportRequest;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.domain.comment.dto.ReportedCommentSummary;
import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.comment.entity.CommentLike;
import site.kkokkio.domain.comment.entity.CommentReport;
import site.kkokkio.domain.comment.repository.CommentLikeRepository;
import site.kkokkio.domain.comment.repository.CommentReportRepository;
import site.kkokkio.domain.comment.repository.CommentRepository;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.global.enums.ReportProcessingStatus;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.exception.ServiceException;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final CommentLikeRepository commentLikeRepository;
	private final CommentReportRepository commentReportRepository;
	private final MemberService memberService;

	public Page<CommentDto> getCommentListByPostId(Long postId, UserDetails userDetails, Pageable pageable) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 포스트입니다."));

		return commentRepository.findAllByPostAndDeletedAtIsNull(post, pageable)
			.map(comment -> CommentDto.from(comment, isLikedByMe(userDetails, comment)));
	}

	public Boolean isLikedByMe(UserDetails userDetails, Comment comment) {
		if (userDetails == null) {
			return null;
		}

		Member member = memberService.findByEmail(userDetails.getUsername());
		return commentLikeRepository.existsByCommentAndMember(comment, member);
	}

	@Transactional
	public CommentDto createComment(Long postId, UserDetails userDetails, CommentCreateRequest request) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 포스트입니다."));

		Member member = memberService.findByEmail(userDetails.getUsername());

		Comment comment = Comment.builder()
			.post(post)
			.member(member)
			.body(request.body())
			.build();

		return CommentDto.from(commentRepository.save(comment));
	}

	@Transactional
	public CommentDto updateComment(Long commentId, CommentCreateRequest request) {
		Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		comment.updateBody(request.body());
		commentRepository.save(comment);

		return CommentDto.from(comment);
	}

	@Transactional
	public void deleteCommentById(Long commentId) {
		Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		comment.softDelete();
		commentRepository.save(comment);
	}

	@Transactional
	public CommentDto likeComment(Long commentId, UserDetails userDetails) {
		Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		Member member = memberService.findByEmail(userDetails.getUsername());

		if (comment.getMember().getEmail().equals(userDetails.getUsername())) {
			throw new ServiceException("403", "본인 댓글은 좋아요 할 수 없습니다.");
		}

		if (commentLikeRepository.existsByCommentAndMember(comment, member)) {
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
	public CommentDto unlikeComment(Long commentId, UserDetails userDetails) {
		Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		Member member = memberService.findByEmail(userDetails.getUsername());
		if (comment.getMember().getEmail().equals(userDetails.getUsername())) {
			throw new ServiceException("403", "본인 댓글은 좋아요 할 수 없습니다.");
		}

		if (commentLikeRepository.existsByCommentAndMember(comment, member)) {
			commentLikeRepository.deleteByCommentAndMember(comment, member);
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
	 * @param userDetails 신고하는 사용자 (인증된 사용자)
	 * @param request 신고 정보 DTO
	 */
	@Transactional
	public void reportComment(Long commentId, UserDetails userDetails, CommentReportRequest request) {

		// 1. 신고 대상 댓글 조회
		Comment comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 댓글입니다."));

		Member reporter = memberService.findByEmail(userDetails.getUsername());

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

		// 5. 기타 사유 선택 시 필수 입력 검증
		if (request.reason() == ReportReason.ETC) {
			// etcReason이 null이거나, 공백만 있거나, 비어있으면 오류
			if (request.etcReason() == null || request.etcReason().trim().isEmpty()) {
				throw new ServiceException("400", "기타 사유 선택 시 상세 내용을 입력해야 합니다.");
			}
		}

		// 6. 신고 정보 생성
		CommentReport commentReport = CommentReport.builder()
			.comment(comment)
			.reporter(reporter)
			.reason(request.reason())
			.etcReason(request.reason() == ReportReason.ETC ? request.etcReason().trim() : null)
			.build();

		// 7. 신고 정보 저장
		commentReportRepository.save(commentReport);

		// 8. 댓글의 신고 카운트 증가 및 저장
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
	public Page<ReportedCommentSummary> getReportedCommentsList(
		Pageable pageable, String searchTarget, String searchValue) {

		// 1. 정렬 옵션 검증 및 매핑
		Map<String, String> sortPropertyMapping = new HashMap<>();
		sortPropertyMapping.put("reportedAt", "latestReportedAt");
		sortPropertyMapping.put("reportCount", "reportCount");

		Sort newSort = Sort.unsorted();

		// Pageable의 Sort 객체 순회하며 개별 정렬 Order 처리
		for (Sort.Order order : pageable.getSort()) {
			String property = order.getProperty();
			Sort.Direction direction = order.getDirection();

			String sqlProperty = sortPropertyMapping.get(property);

			// 허용되지 않은 정렬 속성이면 오류 발생
			if (sqlProperty == null) {
				throw new ServiceException("400", "부적절한 정렬 옵션입니다.");
			}
			newSort = newSort.and(Sort.by(direction, sqlProperty));
		}

		// 최종 Pageable 객체 생성
		Pageable repositoryPageable = PageRequest.of(
			pageable.getPageNumber(),
			pageable.getPageSize(),
			newSort
		);

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
				case "post_title" -> searchPostTitle = trimmedSearchValue;
				case "comment_body" -> searchCommentBody = trimmedSearchValue;
				case "report_reason" -> searchReportReason = trimmedSearchValue;
				default -> throw new ServiceException("400", "부적절한 검색 옵션입니다.");
			}
		}

		// 3. ReportedCommentRepository 메서드 호출 및 반환
		// 매핑된 검색 인자들과 정렬/페이징 정보(repositoryPageable)를 넘겨 호출
		return commentReportRepository.findReportedCommentSummary(
			searchNickname,
			searchPostTitle,
			searchCommentBody,
			searchReportReason,
			repositoryPageable
		);
	}

	/**
	 * 관리자용 신고된 댓글들을 소프트 삭제(숨김) 처리합니다.
	 * @param commentIds 숨길 댓글 ID 목록
	 */
	@Transactional
	public void hideReportedComment(List<Long> commentIds) {

		// 1. 요청된 ID 목록이 비어있는지 확인
		if (commentIds == null || commentIds.isEmpty()) {
			throw new ServiceException("400", "삭제할 댓글 ID가 제공되지 않았습니다.");
		}

		// 2. 이미 삭제(숨김)된 댓글인지 확인
		List<Comment> existingComments = commentRepository.findAllById(commentIds);

		if (existingComments.size() != commentIds.size()) {
			throw new ServiceException("404", "존재하지 않는 댓글이 포함되어 있습니다.");
		}
		// 3. 요청된 commentIds 중 실제로 신고된 댓글의 개수를 확인
		long reportedCommentCount = commentReportRepository.countByCommentIdIn(commentIds);

		// 4. 요청된 commentIds의 개수와 실제로 신고된 댓글의 개수가 다르면 에러 처리
		if (reportedCommentCount != existingComments.size()) {
			throw new ServiceException("400", "신고되지 않은 댓글이 요청에 포함되어 있습니다.");
		}

		// 5. 각 댓글을 숨김 처리 및 신고 상태 변경
		for (Long commentId : commentIds) {
			Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new ServiceException("404", "내부 오류: 댓글을 찾을 수 없습니다."));

			// 이미 삭제된 댓글인지 확인
			if (comment.isDeleted()) {
				throw new ServiceException("400", "ID [" + commentId + "] 댓글은 이미 삭제되었습니다.");
			}

			comment.softDelete();
			commentRepository.save(comment);
		}

		// 5. 요청된 댓글 ID들에 해당하는 모든 신고 엔티티의 상태를 ACCEPTED로 업데이트
		commentReportRepository.updateStatusByCommentIdIn(commentIds, ReportProcessingStatus.ACCEPTED);
	}

	/**
	 * 관리자용 신고된 댓글들의 신고를 거부(삭제) 처리합니다.
	 * @param commentIds 신고를 거부할 댓글 ID 목록
	 */
	@Transactional
	public void rejectReportedComment(List<Long> commentIds) {

		// 1. 요청된 ID 목록이 비어있는지 확인
		if (commentIds == null || commentIds.isEmpty()) {
			throw new ServiceException("400", "신고 거부할 댓글 ID가 제공되지 않았습니다.");
		}
		// 2. 요청된 모든 댓글 ID에 해당하는 Comment 엔티티들이 실제로 존재하는지 확인
		List<Comment> existingComments = commentRepository.findAllById(commentIds);

		if (existingComments.size() != commentIds.size()) {
			throw new ServiceException("404", "존재하지 않는 댓글이 포함되어 있습니다.");
		}

		// 3. 요청된 commentIds 중 실제로 신고된 댓글의 개수를 확인
		long reportedCommentCount = commentReportRepository.countByCommentIdIn(commentIds);

		// 4. 요청된 commentIds의 개수와 실제로 신고된 댓글의 개수가 다르면 에러 처리
		if (reportedCommentCount != existingComments.size()) {
			throw new ServiceException("400", "신고되지 않은 댓글이 요청에 포함되어 있습니다. 신고된 댓글만 거부할 수 있습니다.");
		}

		// 5. 모든 검증을 통과했다면, 요청된 댓글 ID들에 해당하는 모든 신고 엔티티의 상태를 변경
		commentReportRepository.updateStatusByCommentIdIn(commentIds, ReportProcessingStatus.REJECTED);
	}
}
