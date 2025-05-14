package site.kkokkio.domain.member.controller.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.enums.MemberRole;

@Builder
public record AdminMemberResponse(
	@NonNull UUID memberId,
	@NonNull String nickname,
	@NonNull String email,
	@NonNull MemberRole role
) {
	// Member 엔티티로부터 AdminMemberDto를 생성하는 팩토리 메소드
	public static AdminMemberResponse from(Member member) {
		return AdminMemberResponse.builder()
			.memberId(member.getId())
			.nickname(member.getNickname())
			.email(member.getEmail())
			.role(member.getRole())
			.build();
	}
}
