package site.kkokkio.domain.post.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import site.kkokkio.global.enums.ReportReason;

@Builder
public record PostReportRequest(
	@NotNull
	ReportReason reason
) {
}
