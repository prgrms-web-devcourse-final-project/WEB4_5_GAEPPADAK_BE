package site.kkokkio.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.post.dto.PostReportRequestDto;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;
import site.kkokkio.global.security.CustomUserDetails;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@Tag(name = "Post API V2", description = "포스트 관련 API 엔드포인트 V2")
public class PostControllerV2 {
	private final PostService postService;

	@Operation(summary = "포스트 신고")
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
			ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
			ErrorCode.POST_NOT_FOUND_3,
			ErrorCode.REPORT_POST_BAD_REQUEST,
			ErrorCode.REPORT_REASON_BAD_REQUEST,
			ErrorCode.REPORT_POST_DUPLICATE
	})
	@PostMapping("/reports/posts/{postId}")
	@ResponseStatus(HttpStatus.OK)
	public RsData<Void> reportPost(
			@PathVariable("postId") Long postId,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody PostReportRequestDto request
	) {
		// CustomUserDetails에서 Member 엔티티를 가져오는 로직
		Member reporter = userDetails.getMember();

		// Service 메서드 호출
		postService.reportPost(postId, reporter, request.reason());

		return new RsData<>(
				"200",
				"정상적으로 포스트 신고가 접수 되었습니다."
		);
	}
}
