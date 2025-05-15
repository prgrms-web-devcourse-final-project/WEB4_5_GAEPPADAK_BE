package site.kkokkio.domain.report.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import site.kkokkio.global.enums.ReportReason;

@Builder
public record PostReportRequestDto(
        @NotNull
        ReportReason reason
) {
}
