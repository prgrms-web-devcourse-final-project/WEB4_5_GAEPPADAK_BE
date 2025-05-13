package site.kkokkio.domain.comment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.comment.dto.CommentReportRequestDto;
import site.kkokkio.domain.comment.service.CommentService;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.annotations.IsActiveMember;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@Tag(name = "Comment API V2", description = "댓글 관련 API 엔드포인트 V2")
public class CommentControllerV2 {
	private final CommentService commentService;

	@Operation(summary = "댓글 신고")
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.COMMENT_NOT_FOUND,
		ErrorCode.REPORT_COMMENT_BAD_REQUEST,
		ErrorCode.REPORT_COMMENT_FORBIDDEN,
		ErrorCode.REPORT_REASON_BAD_REQUEST,
		ErrorCode.REPORT_COMMENT_DUPLICATE
	})
	@PostMapping("/reports/comments/{commentId}")
	@ResponseStatus(HttpStatus.OK)
	@IsActiveMember
	public RsData<Void> reportComment(
		@PathVariable("commentId") Long commentId,
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody CommentReportRequestDto request
	) {
		// CustomUserDetails에서 Member 엔티티를 가져오는 로직
		Member reporter = userDetails.getMember();

		// Service 메서드 호출
		commentService.reportComment(commentId, reporter, request.reason());

		return new RsData<>(
			"200",
			"정상적으로 댓글 신고가 접수 되었습니다."
		);
	}
}
