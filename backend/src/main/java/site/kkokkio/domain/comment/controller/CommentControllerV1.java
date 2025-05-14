package site.kkokkio.domain.comment.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.controller.dto.CommentListResponse;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.domain.comment.service.CommentService;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.annotations.IsActiveMember;
import site.kkokkio.global.auth.annotations.IsCommentActiveOwner;
import site.kkokkio.global.auth.annotations.IsCommentOwner;
import site.kkokkio.global.dto.Empty;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Comment API V1", description = "댓글 관련 API 엔드포인트 V1")
public class CommentControllerV1 {
	private final CommentService commentService;

	@Operation(summary = "댓글 목록 조회")
	@ApiErrorCodeExamples({ErrorCode.POST_NOT_FOUND_3})
	@GetMapping("/posts/{postId}/comments")
	public RsData<CommentListResponse> getCommentList(
		@PathVariable("postId") Long postId,
		@ParameterObject @PageableDefault(sort = "likeCount") Pageable pageable
	) {
		Page<CommentDto> comments = commentService.getCommentListByPostId(postId, pageable);

		CommentListResponse commentListResponse = CommentListResponse.from(comments);

		return new RsData<>(
			"200",
			"댓글 목록 조회 완료",
			commentListResponse
		);
	}

	@Operation(summary = "댓글 작성")
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH, ErrorCode.POST_NOT_FOUND_3})
	@PostMapping("/posts/{postId}/comments")
	@IsActiveMember
	public RsData<CommentDto> createComment(
		@PathVariable("postId") Long postId,
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody CommentCreateRequest request) {
		CommentDto comment = commentService.createComment(postId, userDetails.getMember(), request);
		return new RsData<>(
			"200",
			"댓글이 등록되었습니다.",
			comment
		);
	}

	@Operation(summary = "댓글 수정")
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.COMMENT_UPDATE_FORBIDDEN, ErrorCode.COMMENT_NOT_FOUND})
	@PatchMapping("/comments/{commentId}")
	@IsCommentActiveOwner
	public RsData<CommentDto> updateComment(
		@PathVariable("commentId") Long commentId,
		@Valid @RequestBody CommentCreateRequest request
	) {
		CommentDto comment = commentService.updateComment(commentId, request);
		return new RsData<>(
			"200",
			"댓글이 수정되었습니다.",
			comment
		);
	}

	@Operation(summary = "댓글 삭제")
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.COMMENT_DELETE_FORBIDDEN, ErrorCode.COMMENT_NOT_FOUND})
	@DeleteMapping("/comments/{commentId}")
	@IsCommentOwner
	public RsData<Empty> deleteComment(
		@PathVariable("commentId") Long commentId
	) {
		commentService.deleteCommentById(commentId);
		return new RsData<>(
			"200",
			"댓글이 삭제되었습니다."
		);
	}

	@Operation(summary = "댓글 좋아요")
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.COMMENT_LIKE_BAD_REQUEST, ErrorCode.COMMENT_LIKE_FORBIDDEN,
		ErrorCode.COMMENT_NOT_FOUND})
	@PostMapping("/comments/{commentId}/like")
	@IsActiveMember
	public RsData<CommentDto> likeComment(
		@PathVariable("commentId") Long commentId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		CommentDto comment = commentService.likeComment(commentId, userDetails.getMember());
		return new RsData<>(
			"200",
			"좋아요가 정상 처리되었습니다.",
			comment
		);
	}

	@Operation(summary = "댓글 좋아요 취소")
	@ApiErrorCodeExamples({ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.COMMENT_UNLIKE_BAD_REQUEST, ErrorCode.COMMENT_LIKE_FORBIDDEN, ErrorCode.COMMENT_NOT_FOUND})
	@DeleteMapping("/comments/{commentId}/like")
	@IsActiveMember
	public RsData<CommentDto> unlikeComment(
		@PathVariable("commentId") Long commentId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		CommentDto comment = commentService.unlikeComment(commentId, userDetails.getMember());
		return new RsData<>(
			"200",
			"좋아요가 취소되었습니다.",
			comment
		);
	}
}
