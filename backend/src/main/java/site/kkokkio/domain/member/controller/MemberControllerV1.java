package site.kkokkio.domain.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.controller.dto.MemberUpdateRequest;
import site.kkokkio.domain.member.service.AuthService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.annotations.IsSelf;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;

@Tag(name = "Member API", description = "회원 관련 기능을 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member")
public class MemberControllerV1 {

	private final MemberService memberService;
	private final AuthService authService;

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
	@ApiErrorCodeExamples({ErrorCode.MISSING_TOKEN, ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.UNSUPPORTED_TOKEN, ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH})
	@GetMapping("/me")
	@IsSelf
	public RsData<MemberResponse> getMember(@AuthenticationPrincipal CustomUserDetails userDetails) {
		MemberResponse memberInfo = memberService.getMemberInfo(userDetails.getUsername());
		return new RsData<>(
			"200",
			"마이페이지 조회 성공",
			memberInfo
		);
	}

	// 회원 정보 수정
	@Operation(summary = "회원수정")
	@ApiErrorCodeExamples({ErrorCode.MISSING_TOKEN, ErrorCode.TOKEN_EXPIRED, ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.UNSUPPORTED_TOKEN, ErrorCode.MALFORMED_TOKEN, ErrorCode.CREDENTIALS_MISMATCH})
	@PatchMapping("/me")
	public RsData<MemberResponse> modifyMember(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestBody @Valid MemberUpdateRequest requestBody
	) {
		MemberResponse memberInfo = memberService.modifyMemberInfo(userDetails, requestBody);
		return new RsData<>(
			"200",
			"회원정보가 정상적으로 수정되었습니다.",
			memberInfo
		);
	}

	// 회원 탈퇴
	@Operation(summary = "회원 탈퇴")
	@ApiErrorCodeExamples({})
	@DeleteMapping("me")
	public RsData<Void> deleteMember(HttpServletRequest request, HttpServletResponse response,
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		memberService.deleteMember(userDetails.getMember());
		authService.logout(request, response);
		return new RsData<>(
			"200",
			"회원이 삭제 되었습니다."
		);
	}
}
