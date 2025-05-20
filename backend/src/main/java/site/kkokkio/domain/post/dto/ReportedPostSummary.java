package site.kkokkio.domain.post.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import site.kkokkio.global.enums.ReportProcessingStatus;

@Builder
public record ReportedPostSummary(
	Long postId,
	String title,
	String summary,
	Long keywordId,
	String keyword,
	String reportReason,
	LocalDateTime latestReportedAt,
	int reportCount,
	ReportProcessingStatus status
) {
}
