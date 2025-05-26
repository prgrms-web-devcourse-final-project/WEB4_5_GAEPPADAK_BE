package site.kkokkio.domain.post.dto;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.post.entity.Post;

@Builder
public record PostDto(
	@NonNull Long postId,
	@NonNull String keyword,
	@NonNull String title,
	@NonNull String summary,
	String thumbnailUrl,
	Boolean reportedByMe
) {
	public static PostDto from(Post post, String keyword) {
		return PostDto.builder()
			.postId(post.getId())
			.keyword(keyword)
			.title(post.getTitle())
			.summary(post.getSummary())
			.thumbnailUrl(post.getThumbnailUrl() == null ? "" : post.getThumbnailUrl())
			.reportedByMe(null)
			.build();
	}

	public static PostDto from(Post post, String keyword, Boolean reportedByMe) {
		return PostDto.builder()
			.postId(post.getId())
			.keyword(keyword)
			.title(post.getTitle())
			.summary(post.getSummary())
			.thumbnailUrl(post.getThumbnailUrl() == null ? "" : post.getThumbnailUrl())
			.reportedByMe(reportedByMe)
			.build();
	}
}
