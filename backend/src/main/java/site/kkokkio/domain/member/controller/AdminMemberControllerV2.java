package site.kkokkio.domain.member.controller;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.controller.dto.AdminMemberDto;
import site.kkokkio.domain.member.controller.dto.AdminMemberListResponse;
import site.kkokkio.domain.member.controller.dto.AdminMemberRoleUpdateRequest;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.service.AuthService;
import site.kkokkio.domain.member.service.MailService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.auth.annotations.IsAdmin;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;
import site.kkokkio.global.util.JwtUtils;

@Tag(name = "Admin Member API", description = "관리자 - 회원 관련 기능을 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/admin/members")
public class AdminMemberControllerV2 {

	private final MemberService memberService;
	private final AuthService authService;
	private final JwtUtils jwtUtils;
	private final MailService mailService;

	@Operation(
		summary = "회원 목록 조회",
		description = "관리자용 회원 목록을 페이징, 정렬, 검색하여 조회합니다.")
	@IsAdmin
	@ApiErrorCodeExamples({
		ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN,
		ErrorCode.MISSING_TOKEN,
		ErrorCode.TOKEN_EXPIRED,
		ErrorCode.CREDENTIALS_MISMATCH
	})
	@GetMapping
	public RsData<AdminMemberListResponse> getMemberList(
		@ParameterObject @PageableDefault(
			sort = "nickname",
			direction = Sort.Direction.ASC
		) Pageable pageable,
		@Parameter(description = "검색 대상 필드 (nickname, email, role)", example = "nickname")
		@RequestParam(value = "searchTarget", required = false) String searchTarget,
		@Parameter(description = "검색어", example = "testuser")
		@RequestParam(value = "searchValue", required = false) String searchValue
	) {
		// MemberService의 관리자용 회원 목록 조회 메소드 호출
		Page<Member> memberPage = memberService.getAdminMemberList(pageable, searchTarget, searchValue);

		// Service에서 반환된 Page<Member> 객체를 AdminMemberListResponse DTO로 변환
		AdminMemberListResponse response = AdminMemberListResponse.from(memberPage);

		return new RsData<>(
			"200",
			"사용자 목록이 조회되었습니다.",
			response
		);
	}

	@Operation(
		summary = "회원 역할 변경",
		description = "관리자가 특정 회원의 역할을 USER 또는 BLACK으로 변경합니다."
	)
	@IsAdmin
	@ApiErrorCodeExamples({
		ErrorCode.UNSUPPORTED_TOKEN,
		ErrorCode.MALFORMED_TOKEN,
		ErrorCode.MISSING_TOKEN,
		ErrorCode.TOKEN_EXPIRED,
		ErrorCode.CREDENTIALS_MISMATCH,
		ErrorCode.EMAIL_NOT_FOUND
	})
	@PatchMapping("/{memberId}")
	public RsData<AdminMemberDto> changeMemberRole(
		@Parameter(description = "역할을 변경할 회원 ID", example = "123e4567")
		@PathVariable UUID memberId,
		@RequestBody AdminMemberRoleUpdateRequest requestDto
	) {
		// Service 레이어 호출
		Member updatedMember = memberService.updateMemberRole(memberId, requestDto.role());

		// 성공 응답의 data 부분으로 사용할 DTO 생성
		AdminMemberDto responseDto = AdminMemberDto.from(updatedMember);

		return new RsData<>(
			"200",
			"사용자 권한이 수정되었습니다.",
			responseDto
		);
	}
}
