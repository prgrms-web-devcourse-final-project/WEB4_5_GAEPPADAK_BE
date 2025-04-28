package site.kkokkio.domain.member.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.enums.MemberRole;

public record MemberLoginResponse(
	UUID id,
	String nickname,
	String email,
	LocalDateTime deleteAt,
	MemberRole role,
	String token
) {
	public static MemberLoginResponse of(Member member, String token) {
		return new MemberLoginResponse(member.getId(), member.getNickname(), member.getEmail(),
			member.getDeletedAt(), member.getRole(), token);
	}
}
