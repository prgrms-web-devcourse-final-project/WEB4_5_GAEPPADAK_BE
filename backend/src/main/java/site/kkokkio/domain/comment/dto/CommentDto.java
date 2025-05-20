package site.kkokkio.domain.comment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.comment.entity.Comment;

@Builder
public record CommentDto(
	@NonNull Long commentId,
	@NonNull UUID memberId,
	@NonNull String profileUrl,
	@NonNull String nickname,
	@NonNull String body,
	@NonNull Integer likeCount,
	@NonNull LocalDateTime createdAt
) {
	public static CommentDto from(Comment comment) {
		String nickname = comment.getMember().getDeletedAt() != null
			? "탈퇴한 회원"
			: comment.getMember().getNickname();
		return CommentDto.builder()
			.commentId(comment.getId())
			.memberId(comment.getMember().getId())
			.profileUrl("https://i.sstatic.net/l60Hf.png")
			.nickname(nickname)
			.body(comment.getBody())
			.likeCount(comment.getLikeCount())
			.createdAt(comment.getCreatedAt())
			.build();
	}
}
