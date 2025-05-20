package site.kkokkio.domain.comment.dto;

public record ReportedCommentSummary(
	Long commentId,
	String memberId,
	String nickname,
	Integer isDeletedMember,
	Long postId,
	String postTitle,
	String commentBody,
	String reportReasons,
	String latestReportedAt,
	Long reportCount,
	String status
) {
}
