package site.kkokkio.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberLoginRequest(
	// 이메일
	@NotBlank(message = "email을 입력해주세요.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email,

	// 비밀번호
	@NotBlank(message = "비밀번호를 입력해주세요.")
	String passwordHash
) {
}
