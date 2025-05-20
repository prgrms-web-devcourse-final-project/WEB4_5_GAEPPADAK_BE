package site.kkokkio.domain.post.controller.dto;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.post.dto.ReportedPostSummary;
import site.kkokkio.global.enums.ReportProcessingStatus;

@Builder
public record ReportedPostResponse(
	@NonNull Long postId,
	@NonNull String title,
	@NonNull String summary,
	Long keywordId,
	String keyword,
	@NonNull List<String> reportReason,
	@NonNull String reportedAt,
	int reportCount,
	@NonNull ReportProcessingStatus status
) {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	// ReportedPostSummary로부터 ReportedPostResponse DTO 생성
	public static ReportedPostResponse from(ReportedPostSummary postSummary) {
		// Summary의 콤마 구분 문자열(String)을 받아서 List<String>으로 변환
		String reportedReosonsString = postSummary.reportReason();
		List<String> reasonStrings;

		if (reportedReosonsString != null && !reportedReosonsString.trim().isEmpty()) {
			reasonStrings = Arrays.asList(reportedReosonsString.split(","));
		} else {
			// 신고 사유가 없으면 빈 리스트 반환
			reasonStrings = List.of();
		}

		// 신고 시각 형식화
		String formattedReportedAt = postSummary.latestReportedAt() != null
			? postSummary.latestReportedAt().format(FORMATTER)
			: null;

		// keywordId와 keyword는 Summary에서 nullable이므로 그대로 매핑
		return ReportedPostResponse.builder()
			.postId(postSummary.postId())
			.title(postSummary.title())
			.summary(postSummary.summary())
			.keywordId(postSummary.keywordId())
			.keyword(postSummary.keyword())
			.reportReason(reasonStrings)
			.reportedAt(formattedReportedAt)
			.reportCount(postSummary.reportCount())
			.status(postSummary.status())
			.build();
	}
}
