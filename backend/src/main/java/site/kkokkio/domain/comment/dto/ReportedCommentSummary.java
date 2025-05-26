package site.kkokkio.domain.comment.dto;

import java.sql.Timestamp;

public record ReportedCommentSummary(
	Long commentId,
	String memberId,
	String nickname,
	Integer isDeletedMember,
	Long postId,
	String postTitle,
	String commentBody,
	String reportReasons,
	Timestamp latestReportedAt,
	Long reportCount,
	String status
) {
}
