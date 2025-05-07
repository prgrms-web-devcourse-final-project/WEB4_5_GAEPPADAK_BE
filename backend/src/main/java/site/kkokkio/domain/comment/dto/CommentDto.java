package site.kkokkio.domain.comment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.comment.entity.Comment;

@Builder
public record CommentDto(
	@NonNull Long id,
	@NonNull UUID memberId,
	@NonNull String nickname,
	@NonNull String body,
	@NonNull Integer likeCount,
	@NonNull LocalDateTime createdAt
) {
	public static CommentDto from(Comment comment) {
		return CommentDto.builder()
			.id(comment.getId())
			.memberId(comment.getMember().getId())
			.nickname(comment.getMember().getNickname())
			.body(comment.getBody())
			.likeCount(comment.getLikeCount())
			.createdAt(comment.getCreatedAt())
			.build();
	}
}
