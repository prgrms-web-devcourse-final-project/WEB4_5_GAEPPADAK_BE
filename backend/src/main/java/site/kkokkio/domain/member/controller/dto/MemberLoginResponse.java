package site.kkokkio.domain.member.controller.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.enums.MemberRole;

@Builder
public record MemberLoginResponse(
	String nickname,
	String email,
	LocalDateTime deleteAt,
	MemberRole role,
	@JsonIgnore
	String token
) {
	public static MemberLoginResponse of(Member member, String token) {
		return new MemberLoginResponse(member.getNickname(), member.getEmail(),
			member.getDeletedAt(), member.getRole(), token);
	}
}
