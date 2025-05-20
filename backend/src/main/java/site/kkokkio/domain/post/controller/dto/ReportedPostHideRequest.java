package site.kkokkio.domain.post.controller.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ReportedPostHideRequest(
	@NotNull(message = "포스트 ID 목록은 null일 수 없습니다.")
	@NotEmpty(message = "포스트가 선택되지 않았습니다.")
	List<Long> postIds
) {
}
