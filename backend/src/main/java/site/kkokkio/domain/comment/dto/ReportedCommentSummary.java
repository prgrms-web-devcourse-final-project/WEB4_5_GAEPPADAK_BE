package site.kkokkio.domain.comment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportedCommentSummary(
	Long commentId,
	UUID memberId,
	String nickname,
	boolean isDeletedMember,
	Long postId,
	String postTitle,
	String commentBody,
	String reportReasons,
	LocalDateTime latestReportedAt,
	int reportCount
) {
}
