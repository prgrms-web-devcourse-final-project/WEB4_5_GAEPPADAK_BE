package site.kkokkio.domain.member.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.controller.dto.MemberLoginRequest;
import site.kkokkio.domain.member.controller.dto.MemberLoginResponse;
import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.dto.EmailVerificationRequest;
import site.kkokkio.domain.member.dto.TokenResponse;
import site.kkokkio.domain.member.service.AuthService;
import site.kkokkio.domain.member.service.MailService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;
import site.kkokkio.global.util.JwtUtils;

@Tag(name = "Member API", description = "회원 관련 기능을 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class MemberControllerV1 {

	private final MemberService memberService;
	private final AuthService authService;
	private final JwtUtils jwtUtils;
	private final MailService mailService;

	// 회원가입
	@Operation(summary = "회원가입")
	@ApiErrorCodeExamples({ErrorCode.EMAIL_ALREADY_EXIST, ErrorCode.NICKNAME_ALREADY_EXIST})
	@PostMapping("/signup")
	public RsData<MemberResponse> createMember(@RequestBody @Validated MemberSignUpRequest request) {
		MemberResponse memberResponse = memberService.createMember(request);
		return new RsData<>("200", "회원가입이 완료되었습니다.", memberResponse);
	}

	// 로그인
	@Operation(summary = "로그인")
	@ApiErrorCodeExamples({ErrorCode.EMAIL_NOT_FOUND, ErrorCode.PASSWORD_UNAUTHORIZED})
	@PostMapping("/login")
	public RsData<MemberLoginResponse> login(
		@RequestBody @Validated MemberLoginRequest request,
		HttpServletResponse response
	) {

		// 로그인 서비스 호출
		MemberLoginResponse loginResponse = authService.login(request.email(), request.passwordHash(), response);

		// JWT 토큰 쿠키에 설정
		jwtUtils.setJwtInCookie(loginResponse.token(), response);
		//
		jwtUtils.setRefreshTokenInCookie(loginResponse.refreshToken(), response);

		return new RsData<>("200", "로그인 성공", loginResponse);
	}

	@Operation(summary = "토큰 재발급")
	@PostMapping("/refresh")
	public RsData<TokenResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
		TokenResponse tokenResponse = authService.refreshToken(request, response);
		return new RsData<>("200", "토큰이 재발급되었습니다.", tokenResponse);
	}

	@Operation(summary = "로그아웃")
	@PostMapping("/logout")
	public RsData<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		authService.logout(request, response);
		return new RsData<>("200", "로그아웃 되었습니다.");
	}

	@Operation(summary = "이메일 인증 코드 전송")
	@PostMapping("/verify-email")
	public RsData<Void> requestAuthCode(@RequestParam String email) throws MessagingException {

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
