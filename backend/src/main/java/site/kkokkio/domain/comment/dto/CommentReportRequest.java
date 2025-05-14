package site.kkokkio.domain.comment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import site.kkokkio.global.enums.ReportReason;

@Builder
public record CommentReportRequest(
	@NotNull
	ReportReason reason
) {
}
