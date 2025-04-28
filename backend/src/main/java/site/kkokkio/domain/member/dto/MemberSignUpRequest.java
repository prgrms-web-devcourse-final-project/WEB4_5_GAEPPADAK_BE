package site.kkokkio.domain.member.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberSignUpRequest(

	// 이메일
	@NotBlank(message = "email을 입력해주세요.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email,

	// 비밀번호
	@NotBlank(message = "password를 입력해주세요.")
	@Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.")
	@Pattern(
		regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,20}$",
		message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
	)
	String passwordHash,

	// 닉네임
	@NotBlank(message = "닉네임을 입력해주세요.")
	@Size(min = 2, max = 10, message = "닉네임은 2~10자 사이여야 합니다.")
	String nickname,

	// 생년월일
	@NotBlank(message = "생년월일을 입력해주세요.")
	LocalDate birthDate
) {
}
