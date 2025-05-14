package site.kkokkio.domain.comment.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ReportedCommentHideRequest(
	@NotNull(message = "댓글 ID 목록은 null일 수 없습니다.")
	@NotEmpty(message = "댓글이 선택되지 않았습니다.")
	List<Long> commentIds
) {
}
