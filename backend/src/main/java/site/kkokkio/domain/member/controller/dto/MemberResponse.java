package site.kkokkio.domain.member.controller.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.enums.MemberRole;

@Getter
@AllArgsConstructor
public class MemberResponse {
	private String email;
	private String nickname;
	private LocalDate birthDate;
	private MemberRole role;

	public MemberResponse(Member member) {
		this.email = member.getEmail();
		this.nickname = member.getNickname();
		this.birthDate = member.getBirthDate();
		this.role = member.getRole();
	}
}

