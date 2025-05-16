package site.kkokkio.domain.post.controller;

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
import site.kkokkio.domain.post.controller.dto.PostReportRequest;
import site.kkokkio.domain.post.controller.dto.ReportedPostHideRequest;
import site.kkokkio.domain.post.controller.dto.ReportedPostListResponse;
import site.kkokkio.domain.post.dto.ReportedPostSummary;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.annotations.IsAdmin;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@Tag(name = "Post API V2", description = "포스트 관련 API 엔드포인트 V2")
public class PostControllerV2 {
	private final PostService postService;

	@Operation(
		summary = "포스트 신고",
		description = "댓글을 신고하는 기능입니다."
	)
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.CREDENTIALS_MISMATCH,
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
		@Valid @RequestBody PostReportRequest request
	) {
		// Service 메서드 호출
		postService.reportPost(postId, userDetails, request.reason());

		return new RsData<>(
			"200",
			"정상적으로 포스트 신고가 접수 되었습니다."
		);
	}

	@Operation(
		summary = "신고된 포스트 목록 조회",
		description = "관리자 권한으로 신고된 포스트 목록을 페이징, 정렬, 검색하여 조회합니다."
	)
	@IsAdmin
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.MISSING_TOKEN, ErrorCode.BAD_SEARCH_OPTION,
		ErrorCode.BAD_SORT_OPTION
	})
	@GetMapping("/admin/reports/posts")
	public RsData<ReportedPostListResponse> getReportPosts(
		@ParameterObject
		@PageableDefault(
			size = 10,
			sort = "reportedAt",
			direction = Sort.Direction.DESC
		) Pageable pageable,
		@RequestParam(required = false) String searchTarget,
		@RequestParam(required = false) String searchValue
	) {
		// 서비스 레이어 메서드 호출하여 신고된 포스트 목록 조회
		Page<ReportedPostSummary> reportedPostPage = postService
			.getReportedPostsList(pageable, searchTarget, searchValue);

		// 서비스 결과를 최종 응답 DTO로 변환
		ReportedPostListResponse response = ReportedPostListResponse.from(reportedPostPage);

		return new RsData<>(
			"200",
			"신고된 포스트 목록입니다.",
			response
		);
	}

	@Operation(
		summary = "신고된 포스트 숨김 처리",
		description = "관리자 권한으로 선택된 신고 포스트들을 소프트 삭제(숨김) 처리합니다."
	)
	@IsAdmin
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.MISSING_TOKEN, ErrorCode.POST_IDS_NOT_PROVIDED,
		ErrorCode.POST_NOT_INCLUDE
	})
	@PostMapping("/admin/reports/posts")
	@ResponseStatus(HttpStatus.OK)
	public RsData<Void> hideReportedPosts(
		@Parameter(description = "숨길 포스트 ID 목록을 포함하는 요청 본문", required = true)
		@Valid @RequestBody ReportedPostHideRequest request
	) {
		// 서비스 레이어의 숨김 처리 메서드 호출
		postService.hideReportedPost(request.postIds());

		return new RsData<>(
			"200",
			"포스트가 정상적으로 삭제되었습니다."
		);
	}

	@Operation(
		summary = "신고된 포스트 신고 거부 처리",
		description = "관리자 권한으로 선택된 신고 포스트들의 신고를 거부(삭제) 처리합니다."
	)
	@IsAdmin
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.MISSING_TOKEN, ErrorCode.POST_IDS_NOT_PROVIDED,
		ErrorCode.POST_NOT_INCLUDE
	})
	@DeleteMapping("/admin/reports/posts")
	@ResponseStatus(HttpStatus.OK)
	public RsData<Void> rejectReportedPosts(
		@Parameter(description = "신고 거부할 포스트 ID 목록을 포함하는 요청 본문", required = true)
		@Valid @RequestBody ReportedPostHideRequest request
	) {
		// 서비스 레이어의 신고 거부 처리 메서드 호출
		postService.rejectReportedPost(request.postIds());

		return new RsData<>(
			"200",
			"선택하신 신고가 거부 처리되었습니다."
		);
	}
}
