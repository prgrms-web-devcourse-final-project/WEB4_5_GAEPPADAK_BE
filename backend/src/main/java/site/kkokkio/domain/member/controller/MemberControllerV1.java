package site.kkokkio.domain.member.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.controller.dto.MemberLoginRequest;
import site.kkokkio.domain.member.controller.dto.MemberLoginResponse;
import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.dto.EmailVerificationRequest;
import site.kkokkio.domain.member.service.MailService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.util.JwtUtils;

@Tag(name = "Member API", description = "회원 관련 기능을 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class MemberControllerV1 {

	private final MemberService memberService;
	private final JwtUtils jwtUtils;
	private final MailService mailService;

	// 회원가입
	@Operation(summary = "회원가입")
	@PostMapping("/signup")
	public RsData<MemberResponse> createMember(@RequestBody @Validated MemberSignUpRequest request) {
		MemberResponse memberResponse = memberService.createMember(request);
		return new RsData<>("200", "회원가입이 완료되었습니다.", memberResponse);
	}

	// 로그인
	@Operation(summary = "로그인")
	@PostMapping("/login")
	public RsData<MemberLoginResponse> login(
		@RequestBody @Validated MemberLoginRequest request,
		HttpServletResponse response
	) {

		// 로그인 서비스 호출
		MemberLoginResponse loginResponse = memberService.loginMember(request.email(), request.passwordHash());

		// JWT 토큰 쿠키에 설정
		jwtUtils.setJwtInCookie(loginResponse.token(), response);

		return new RsData<>("200", "로그인 성공", loginResponse);
	}

	@Operation(summary = "이메일 인증 코드 전송")
	@GetMapping("/verify-email")
	public RsData<Void> requestAuthCode(String email) throws MessagingException {

		boolean isSend = mailService.sendAuthCode(email);
		return isSend
			? new RsData<>("200", "인증 코드가 전송되었습니다.")
			: new RsData<>("500", "인증 코드 전송이 실패하였습니다.");
	}

	@Operation(summary = "이메일 인증")
	@PostMapping("/check-email")
	public RsData<Void> validateAuthCode(@RequestBody @Valid EmailVerificationRequest emailVerificationRequestDto) {

		boolean isSuccess = mailService.validationAuthCode(emailVerificationRequestDto);
		return isSuccess
			? new RsData<>("200", "이메일 인증에 성공하였습니다.")
			: new RsData<>("400", "이메일 인증에 실패하였습니다.");
	}

}
