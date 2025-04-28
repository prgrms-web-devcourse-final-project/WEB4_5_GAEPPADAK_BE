package site.kkokkio.domain.comment.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import site.kkokkio.domain.comment.entity.Comment;

@Builder
public record CommentResponse(
	Long id,
	UUID memberId,
	String body,
	Integer likeCount,
	LocalDateTime createdAt
) {
	public static CommentResponse from(Comment comment) {
		return CommentResponse.builder()
			.id(comment.getId())
			.memberId(comment.getMember().getId())
			.body(comment.getBody())
			.likeCount(comment.getLikeCount())
			.createdAt(comment.getCreatedAt())
			.build();
	}
}
