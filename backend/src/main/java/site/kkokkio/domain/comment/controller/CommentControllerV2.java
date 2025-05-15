package site.kkokkio.domain.comment.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.comment.controller.dto.CommentReportRequest;
import site.kkokkio.domain.comment.controller.dto.ReportedCommentHideRequest;
import site.kkokkio.domain.comment.controller.dto.ReportedCommentListResponse;
import site.kkokkio.domain.comment.dto.ReportedCommentSummary;
import site.kkokkio.domain.comment.service.CommentService;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.annotations.IsActiveMember;
import site.kkokkio.global.auth.annotations.IsAdmin;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@Tag(name = "Comment API V2", description = "댓글 관련 API 엔드포인트 V2")
public class CommentControllerV2 {
	private final CommentService commentService;

	@Operation(
		summary = "댓글 신고",
		description = "댓글을 신고하는 기능입니다."
	)
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.COMMENT_NOT_FOUND,
		ErrorCode.REPORT_COMMENT_BAD_REQUEST,
		ErrorCode.REPORT_COMMENT_FORBIDDEN,
		ErrorCode.REPORT_REASON_BAD_REQUEST,
		ErrorCode.REPORT_COMMENT_DUPLICATE,
		ErrorCode.EMAIL_NOT_FOUND
	})
	@PostMapping("/reports/comments/{commentId}")
	@ResponseStatus(HttpStatus.OK)
	@IsActiveMember
	public RsData<Void> reportComment(
		@PathVariable("commentId") Long commentId,
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody CommentReportRequest request
	) {
		// Service 메서드 호출
		commentService.reportComment(commentId, userDetails, request.reason());

		return new RsData<>(
			"200",
			"정상적으로 댓글 신고가 접수 되었습니다."
		);
	}

	@Operation(
		summary = "신고된 댓글 조회",
		description = "관리자 권한으로 신고된 댓글 목록을 확인할 수 있습니다."
	)
	@IsAdmin
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.MISSING_TOKEN, ErrorCode.BAD_SEARCH_OPTION,
		ErrorCode.BAD_SORT_OPTION
	})
	@GetMapping("/admin/reports/comments")
	public RsData<ReportedCommentListResponse> getReportCommentsList(
		@ParameterObject
		@PageableDefault(
			sort = "reportedAt",
			direction = Sort.Direction.DESC
		) Pageable pageable,

		@Parameter(description = "검색 대상 필드", example = "nickname")
		@RequestParam(value = "searchTarget", required = false) String searchTarget,

		@Parameter(description = "검색어", example = "ETC")
		@RequestParam(value = "searchValue", required = false) String searchValue
	) {
		// 서비스 레이어의 신고된 댓글 목록 조회 메서드 호출
		Page<ReportedCommentSummary> reportedCommentPage = commentService
			.getReportedCommentsList(pageable, searchTarget, searchValue);

		// 서비스에서 받은 Page<ReportedCommentSummary> 결과를 최종 응답 DTO에 매핑
		ReportedCommentListResponse response = ReportedCommentListResponse.from(reportedCommentPage);

		return new RsData<>(
			"200",
			"신고된 댓글 목록이 조회되었습니다.",
			response
		);
	}

	@Operation(
		summary = "신고된 댓글 숨김 처리",
		description = "관리자 권한으로 선택된 신고 댓글들을 소프트 삭제(숨김) 처리합니다."
	)
	@IsAdmin
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.MISSING_TOKEN, ErrorCode.COMMENT_IDS_NOT_PROVIDED,
		ErrorCode.COMMENT_NOT_INCLUDE
	})
	@PostMapping("/admin/reports/comments")
	@ResponseStatus(HttpStatus.OK)
	public RsData<Void> hideReportComments(
		@Parameter(description = "숨길 댓글 ID 목록을 포함하는 요청 본문", required = true)
		@Valid @RequestBody ReportedCommentHideRequest request
	) {
		// 서비스 레이어의 숨김 처리 메서드 호출
		commentService.hideReportedComment(request.commentIds());

		return new RsData<>(
			"200",
			"선택하신 댓글이 숨김 처리되었습니다."
		);
	}

	@Operation(
		summary = "신고된 댓글 신고 거부 처리",
		description = "관리자 권한으로 선택된 신고 댓글들의 신고를 거부(삭제) 처리합니다."
	)
	@IsAdmin
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.MISSING_TOKEN, ErrorCode.COMMENT_IDS_NOT_PROVIDED,
		ErrorCode.COMMENT_NOT_INCLUDE
	})
	@DeleteMapping("/admin/reports/comments")
	@ResponseStatus(HttpStatus.OK)
	public RsData<Void> rejectReportedComments(
		@Parameter(description = "신고 거부할 댓글 ID 목록을 포함하는 요청 본문", required = true)
		@Valid @RequestBody ReportedCommentHideRequest request
	) {
		// 서비스 레이어의 신고 거부 메서드 호출
		commentService.rejectReportedComment(request.commentIds());

		return new RsData<>(
			"200",
			"선택하신 신고가 거부 처리되었습니다."
		);
	}
}
