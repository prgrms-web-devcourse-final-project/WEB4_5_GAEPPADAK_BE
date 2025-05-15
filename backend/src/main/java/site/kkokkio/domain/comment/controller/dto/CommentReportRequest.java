<<<<<<<< HEAD:backend/src/main/java/site/kkokkio/domain/comment/controller/dto/CommentReportRequest.java
package site.kkokkio.domain.comment.controller.dto;
========
package site.kkokkio.domain.report.dto;
>>>>>>>> aff77ed (add post report feature and comment report modify):backend/src/main/java/site/kkokkio/domain/report/dto/CommentReportRequestDto.java

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import site.kkokkio.global.enums.ReportReason;

@Builder
public record CommentReportRequest(
	@NotNull
	ReportReason reason
) {
}
