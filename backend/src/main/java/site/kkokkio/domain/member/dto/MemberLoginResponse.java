package site.kkokkio.domain.member.dto;

import java.util.UUID;

import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.enums.MemberRole;

public record MemberLoginResponse(
	UUID id,
	String token,
	String nickname,
	String email,
	MemberRole role
) {
	public static MemberLoginResponse of(Member member, String token) {
		return new MemberLoginResponse(member.getId(), token, member.getNickname(), member.getEmail(),
			member.getRole());
	}
}
