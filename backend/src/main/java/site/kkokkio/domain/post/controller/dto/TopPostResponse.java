package site.kkokkio.domain.post.controller.dto;

import lombok.Builder;

@Builder
public record TopPostResponse(
	Long postId,
	String keyword,
	String title,
	String summary,
	String thumbnailUrl
) {
}
