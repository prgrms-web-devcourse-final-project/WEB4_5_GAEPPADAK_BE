package site.kkokkio.domain.member.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
	// 새 비밀번호
	@NotBlank(message = "새 비밀번호를 입력해주세요.")
	@Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.")
	@Pattern(
		regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,20}$",
		message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
	)
	String newPassword,

	// 비밀 번호 검증
	@NotBlank(message = "비밀번호 확인을 입력해주세요.")
	@Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.")
	@Pattern(
		regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,20}$",
		message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
	)
	@NotBlank @Size(min = 8) String checkPassword
) {
}
