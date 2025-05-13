package site.kkokkio.domain.member.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import site.kkokkio.global.enums.MemberRole;

@Builder
public record AdminMemberRoleUpdateRequest(
	@NotBlank(message = "변경할 역할을 입력해주세요")
	MemberRole role
) {
}
