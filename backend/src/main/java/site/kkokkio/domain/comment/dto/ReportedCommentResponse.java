package site.kkokkio.domain.comment.dto;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record ReportedCommentResponse(
	@NonNull Long commentId,
	@NonNull UUID memberId,
	@NonNull String nickname,
	@NonNull Long postId,
	@NonNull String title,
	@NonNull String body,
	@NonNull List<String> reportReason,
	@NonNull String reportedAt
) {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	// 서비스 레이어의 집계된 데이터 (ReportedCommentServiceDto)로부터 DTO 생성
	public static ReportedCommentResponse from(ReportedCommentSummary service) {
		// 탈퇴 회원 닉네임 처리
		String withdrawNickname = service.isDeletedMember() ? "탈퇴한 회원" : service.nickname();

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
		String formattedReportedAt = service.latestReportedAt() != null
			? service.latestReportedAt().format(FORMATTER)
			: null;

		return ReportedCommentResponse.builder()
			.commentId(service.commentId())
			.memberId(service.memberId())
			.nickname(withdrawNickname)
			.postId(service.postId())
			.title(service.postTitle())
			.body(service.commentBody())
			.reportReason(reasonStrings)
			.reportedAt(formattedReportedAt)
			.build();
	}
}
