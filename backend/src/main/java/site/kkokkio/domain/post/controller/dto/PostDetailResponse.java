package site.kkokkio.domain.post.controller.dto;

import lombok.Builder;

@Builder
public record PostDetailResponse(
	Long postId,
	String keyword,
	String title,
	String summary,
	String thumbnailUrl,
	Boolean reportedByMe
) {
}
