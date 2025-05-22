package site.kkokkio.domain.comment.controller.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.comment.dto.ReportedCommentSummary;
import site.kkokkio.global.enums.ReportProcessingStatus;
import site.kkokkio.global.exception.ServiceException;

@Builder
public record ReportedCommentResponse(
	@NonNull Long commentId,
	@NonNull UUID memberId,
	@NonNull String nickname,
	@NonNull Long postId,
	@NonNull String title,
	@NonNull String body,
	@NonNull List<String> reportReason,
	@NonNull String reportedAt,
	@NonNull Long reportCount,
	@NonNull ReportProcessingStatus status
) {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	// 서비스 레이어의 집계된 데이터 (ReportedCommentServiceDto)로부터 DTO 생성
	public static ReportedCommentResponse from(ReportedCommentSummary service) {
		// 탈퇴 회원 닉네임 처리
		String withdrawNickname = service.isDeletedMember() == 1 ? "탈퇴한 회원" : service.nickname();

		// Summary의 콤마 구분 문자열(String)을 받아서 List<String>으로 변환
		String reportedReasonsString = service.reportReasons();
		List<String> reasonStrings;

		// 콤마로 분할하여 List<String>으로 변환
		if (reportedReasonsString != null && !reportedReasonsString.trim().isEmpty()) {
			reasonStrings = Arrays.asList(reportedReasonsString.split(","));
		} else {
			// 신고 사유 문자열이 null 또는 비어있으면 빈 리스트 반환
			reasonStrings = List.of();
		}

		// 신고 시각 형식화
		String formattedReportedAt = null;
		if (service.latestReportedAt() != null && !service.latestReportedAt().trim().isEmpty()) {
			LocalDateTime parsedReportedAt = LocalDateTime.parse(service.latestReportedAt(), FORMATTER);
			formattedReportedAt = parsedReportedAt.format(FORMATTER);
		}

		// status 변환 로직
		ReportProcessingStatus convertedStatus = null;
		if (service.status() != null && !service.status().trim().isEmpty()) {
			try {
				// ReportedCommentSummary의 String 타입 status를 ReportProcessingStatus Enum으로 변환
				convertedStatus = site.kkokkio.global.enums.ReportProcessingStatus.valueOf(
					service.status().toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new ServiceException("500", "처리할 수 없는 신고 상태입니다:" + service.status());
			}
		} else {
			convertedStatus = ReportProcessingStatus.PENDING;
		}

		return ReportedCommentResponse.builder()
			.commentId(service.commentId())
			.memberId(UUID.fromString(service.memberId()))
			.nickname(withdrawNickname)
			.postId(service.postId())
			.title(service.postTitle())
			.body(service.commentBody())
			.reportReason(reasonStrings)
			.reportedAt(formattedReportedAt)
			.reportCount(service.reportCount())
			.status(convertedStatus)
			.build();
	}
}
