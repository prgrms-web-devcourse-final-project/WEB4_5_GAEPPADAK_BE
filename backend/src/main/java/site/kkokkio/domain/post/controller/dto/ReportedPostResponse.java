package site.kkokkio.domain.post.controller.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.post.dto.ReportedPostSummary;
import site.kkokkio.global.enums.ReportProcessingStatus;
import site.kkokkio.global.exception.ServiceException;

@Builder
public record ReportedPostResponse(
	@NonNull Long postId,
	@NonNull String title,
	@NonNull String summary,
	Long keywordId,
	String keyword,
	@NonNull List<String> reportReason,
	@NonNull String reportedAt,
	Long reportCount,
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
		String formmattedReportedAt = null;
		if (postSummary.latestReportedAt() != null) {
			// String으로 받은 것을 LocalDateTime으로 파싱 후, 다시 원하는 포맷으로 변환
			LocalDateTime parsedDateTime = postSummary.latestReportedAt().toLocalDateTime();
			formmattedReportedAt = parsedDateTime.format(FORMATTER);
		}

		// status String 값을 Enum으로 변환
		ReportProcessingStatus convertedStatus = null;
		if (postSummary.status() != null && !postSummary.status().trim().isEmpty()) {
			try {
				// DB에서 가져온 문자열을 대문자로 변환하여 Enum으로 파싱
				convertedStatus = ReportProcessingStatus.valueOf(postSummary.status());
			} catch (IllegalArgumentException e) {
				// Enum에 해당하지 않는 문자열이 들어올 경우 예외 처리
				throw new ServiceException("500", "처리할 수 없는 신고상태입니다:" + postSummary.status());
			}
		} else {
			convertedStatus = ReportProcessingStatus.PENDING;
		}

		// keywordId와 keyword는 Summary에서 nullable이므로 그대로 매핑
		return ReportedPostResponse.builder()
			.postId(postSummary.postId())
			.title(postSummary.title())
			.summary(postSummary.summary())
			.keywordId(postSummary.keywordId())
			.keyword(postSummary.keyword())
			.reportReason(reasonStrings)
			.reportedAt(formmattedReportedAt)
			.reportCount(postSummary.reportCount())
			.status(convertedStatus)
			.build();
	}
}
