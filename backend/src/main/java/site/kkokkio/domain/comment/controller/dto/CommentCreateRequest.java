package site.kkokkio.domain.comment.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
	@Size(max = 150, message = "댓글을 150자 이하로 작성해주시길 바랍니다.")
	@NotBlank
	String body
) {
}
