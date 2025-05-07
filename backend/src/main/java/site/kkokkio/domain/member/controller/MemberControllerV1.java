package site.kkokkio.domain.member.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
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
@RequestMapping("/api/v1/member")
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

		memberService.siginUpUnverified(memberResponse);

		return new RsData<>("200", "회원가입이 완료되었습니다.", memberResponse);
	}

	// 회원 정보 조회
	@Operation(summary = "회원조회")
	@ApiErrorCodeExamples({ErrorCode.MEMBER_ME_BAD_REQUEST})
	@GetMapping("/me")
	public RsData<MemberResponse> getMember(HttpServletRequest request) {
		MemberResponse memberInfo = memberService.getMemberInfo(request);
		return new RsData<>(
			"200",
			"마이페이지 조회 성공",
			memberInfo
		);
	}
}
