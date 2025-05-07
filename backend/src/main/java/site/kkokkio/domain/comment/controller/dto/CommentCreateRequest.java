package site.kkokkio.domain.comment.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
	@NotBlank String body
) {
}
