package site.kkokkio.domain.post.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import site.kkokkio.global.enums.ReportReason;

@Builder
public record PostReportRequest(
	@NotNull
	ReportReason reason,

	@Size(max = 300, message = "기타 사유는 최대 {max}자까지 입력 가능합니다.")
	String etcReason
) {
}
