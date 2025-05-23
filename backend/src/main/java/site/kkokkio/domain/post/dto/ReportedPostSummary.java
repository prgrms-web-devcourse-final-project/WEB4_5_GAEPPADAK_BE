package site.kkokkio.domain.post.dto;

import java.sql.Timestamp;

import lombok.Builder;

@Builder
public record ReportedPostSummary(
	Long postId,
	String title,
	String summary,
	Long keywordId,
	String keyword,
	String reportReason,
	Timestamp latestReportedAt,
	Long reportCount,
	String status
) {
}
