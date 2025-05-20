package site.kkokkio.domain.post.dto;

import lombok.Builder;

@Builder
public record ReportedPostSummary(
	Long postId,
	String title,
	String summary,
	Long keywordId,
	String keyword,
	String reportReason,
	String latestReportedAt,
	Long reportCount,
	String status
) {
}
