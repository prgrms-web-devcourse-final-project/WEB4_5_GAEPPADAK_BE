package site.kkokkio.domain.member.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(
	// 비밀번호
	@Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.")
	@Pattern(
		regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,20}$",
		message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
	)
	String passwordHash,

	// 닉네임
	@Size(min = 2, max = 10, message = "닉네임은 2~10자 사이여야 합니다.")
	String nickname
) {

}
