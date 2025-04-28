package site.kkokkio.domain.member.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.dto.MemberResponse;
import site.kkokkio.domain.member.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.dto.RsData;

@Tag(name = "Member API", description = "회원 관련 기능을 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class MemberController {

	private final MemberService memberService;

	// 회원가입
	@PostMapping("/signup")
	public RsData<MemberResponse> createMember(@RequestBody @Validated MemberSignUpRequest request) {
		MemberResponse memberResponse = memberService.createMember(request);
		return new RsData<>("200", "회원가입이 완료되었습니다.", memberResponse);
	}
}
