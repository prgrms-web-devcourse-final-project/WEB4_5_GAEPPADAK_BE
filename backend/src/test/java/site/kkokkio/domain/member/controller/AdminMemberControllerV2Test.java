package site.kkokkio.domain.member.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.service.AuthService;
import site.kkokkio.domain.member.service.MailService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.auth.AuthChecker;
import site.kkokkio.global.auth.CustomUserDetailsService;
import site.kkokkio.global.config.SecurityConfig;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(AdminMemberControllerV2.class)
@Import(SecurityConfig.class)
class AdminMemberControllerV2Test {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	AuthChecker authChecker;

	@MockitoBean
	private MemberService memberService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtUtils jwtUtils;

	@MockitoBean
	private MailService mailService;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private RedisTemplate<String, String> redisTemplate;

	@MockitoBean
	private UserDetailsService userDetailsService;

	private final UUID testMemberId = UUID.randomUUID();
	private final UUID adminMemberId = UUID.randomUUID();
	private final UUID nonExistentMemberId = UUID.randomUUID();
	private final String requestBodyRoleBlack = """
		{
		  "role": "BLACK"
		}
		""";
	private final String requestBodyRoleUser = """
		{
		  "role": "USER"
		}
		""";
	private final String requestBodyRoleAdmin = """
		{
		  "role": "ADMIN"
		}
		""";
	private final String requestBodyRoleInvalidRole = """
		{
		  "role": "INVALID"
		}
		""";

	private final Member sampleUserMember = Member.builder()
		.id(UUID.randomUUID())
		.email("user@example.com")
		.nickname("userNickname")
		.role(MemberRole.USER)
		.build();

	private final Member sampleAdminMember = Member.builder()
		.id(UUID.randomUUID())
		.email("admin@example.com")
		.nickname("adminNickname")
		.role(MemberRole.ADMIN)
		.build();

	private final Member sampleBlackMember = Member.builder()
		.id(UUID.randomUUID())
		.email("black@example.com")
		.nickname("blackNickname")
		.role(MemberRole.BLACK)
		.build();

	@Test
	@DisplayName("검색 조건 없이 회원 목록 조회 성공")
	@WithMockUser(roles = "ADMIN")
	void getMemberList_Success_NoSearch() throws Exception {
		/// given
		List<Member> allMembers = Arrays.asList(sampleUserMember, sampleAdminMember, sampleBlackMember);
		Pageable defaultPageable = PageRequest.of(0, 10, Sort.by("nickname").ascending());
		Page<Member> memberPage = new PageImpl<>(allMembers, defaultPageable, allMembers.size());

		given(memberService.getAdminMemberList(eq(defaultPageable), eq(null), eq(null)))
			.willReturn(memberPage);

		/// when & then
		mockMvc.perform(get("/api/v2/admin/members")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("사용자 목록이 조회되었습니다."))
			.andExpect(jsonPath("$.data.list").isArray())
			.andExpect(jsonPath("$.data.list.length()").value(allMembers.size()))
			.andExpect(jsonPath("$.data.list[0].memberId").exists())
			.andExpect(jsonPath("$.data.list[0].nickname").exists())
			.andExpect(jsonPath("$.data.list[0].email").exists())
			.andExpect(jsonPath("$.data.list[0].role").exists())
			.andExpect(jsonPath("$.data.meta.page").value(defaultPageable.getPageNumber()))
			.andExpect(jsonPath("$.data.meta.size").value(defaultPageable.getPageSize()))
			.andExpect(jsonPath("$.data.meta.totalElements").value(memberPage.getTotalElements()))
			.andExpect(jsonPath("$.data.meta.totalPages").value(memberPage.getTotalPages()))
			.andExpect(jsonPath("$.data.meta.hasNext").value(memberPage.hasNext()))
			.andExpect(jsonPath("$.data.meta.hasPrevious").value(memberPage.hasPrevious()));

		// memberService.getAdminMemberList 메서드가 예상대로 호출되었는지 검증
		then(memberService).should(times(1)).getAdminMemberList(eq(defaultPageable), eq(null), eq(null));
	}

	@Test
	@DisplayName("닉네임 검색 조건으로 회원 목록 조회 성공")
	@WithMockUser(roles = "ADMIN")
	void getMemberList_Success_SearchByNickname() throws Exception {
		/// given
		String searchTarget = "nickname";
		String searchValue = "userNickname";
		List<Member> searchedMembers = Arrays.asList(sampleUserMember);
		Pageable pageable = PageRequest.of(0, 10, Sort.by("nickname").ascending());
		Page<Member> memberPage = new PageImpl<>(searchedMembers, pageable, searchedMembers.size());

		given(memberService.getAdminMemberList(eq(pageable), eq(searchTarget), eq(searchValue)))
			.willReturn(memberPage);

		/// when & then
		mockMvc.perform(get("/api/v2/admin/members")
				.param("searchTarget", searchTarget)
				.param("searchValue", searchValue)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("사용자 목록이 조회되었습니다."))
			.andExpect(jsonPath("$.data.list").isArray())
			.andExpect(jsonPath("$.data.list.length()").value(searchedMembers.size()))
			.andExpect(jsonPath("$.data.list[0].memberId").value(sampleUserMember.getId().toString()))
			.andExpect(jsonPath("$.data.list[0].nickname").value(sampleUserMember.getNickname()))
			.andExpect(jsonPath("$.data.meta.totalElements").value(memberPage.getTotalElements()));

		// memberService.getAdminMemberList 메서드가 예상대로 호출되었는지 검증
		then(memberService).should(times(1)).getAdminMemberList(eq(pageable), eq(searchTarget), eq(searchValue));
	}

	@Test
	@DisplayName("이메일 검색 조건으로 회원 목록 조회 성공")
	@WithMockUser(roles = "ADMIN")
	void getMemberList_Success_SearchByEmail() throws Exception {
		/// given
		String searchTarget = "email";
		String searchValue = "admin@example.com";
		List<Member> searchedMembers = Arrays.asList(sampleAdminMember);
		Pageable pageable = PageRequest.of(0, 10, Sort.by("nickname").ascending());
		Page<Member> memberPage = new PageImpl<>(searchedMembers, pageable, searchedMembers.size());

		given(memberService.getAdminMemberList(eq(pageable), eq(searchTarget), eq(searchValue)))
			.willReturn(memberPage);

		/// when & then
		mockMvc.perform(get("/api/v2/admin/members")
				.param("searchTarget", searchTarget)
				.param("searchValue", searchValue)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("사용자 목록이 조회되었습니다."))
			.andExpect(jsonPath("$.data.list").isArray())
			.andExpect(jsonPath("$.data.list.length()").value(searchedMembers.size()))
			.andExpect(jsonPath("$.data.list[0].memberId").value(sampleAdminMember.getId().toString()))
			.andExpect(jsonPath("$.data.list[0].nickname").value(sampleAdminMember.getNickname()))
			.andExpect(jsonPath("$.data.meta.totalElements").value(memberPage.getTotalElements()));

		// memberService.getAdminMemberList 메서드가 예상대로 호출되었는지 검증
		then(memberService).should(times(1)).getAdminMemberList(eq(pageable), eq(searchTarget), eq(searchValue));
	}

	@Test
	@DisplayName("역할 검색 조건으로 회원 목록 조회 성공")
	@WithMockUser(roles = "ADMIN")
	void getMemberList_Success_SearchByRole() throws Exception {
		/// given
		String searchTarget = "role";
		String searchValue = "BLACK";
		List<Member> searchedMembers = Arrays.asList(sampleBlackMember);
		Pageable pageable = PageRequest.of(0, 10, Sort.by("nickname").ascending());
		Page<Member> memberPage = new PageImpl<>(searchedMembers, pageable, searchedMembers.size());

		given(memberService.getAdminMemberList(eq(pageable), eq(searchTarget), eq(searchValue)))
			.willReturn(memberPage);

		/// when & then
		mockMvc.perform(get("/api/v2/admin/members")
				.param("searchTarget", searchTarget)
				.param("searchValue", searchValue)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("사용자 목록이 조회되었습니다."))
			.andExpect(jsonPath("$.data.list").isArray())
			.andExpect(jsonPath("$.data.list.length()").value(searchedMembers.size()))
			.andExpect(jsonPath("$.data.list[0].memberId").value(sampleBlackMember.getId().toString()))
			.andExpect(jsonPath("$.data.list[0].nickname").value(sampleBlackMember.getNickname()))
			.andExpect(jsonPath("$.data.meta.totalElements").value(memberPage.getTotalElements()));

		// memberService.getAdminMemberList 메서드가 예상대로 호출되었는지 검증
		then(memberService).should(times(1)).getAdminMemberList(eq(pageable), eq(searchTarget), eq(searchValue));
	}

	@Test
	@DisplayName("ADMIN 권한 없는 사용자가 접근")
	@WithMockUser(roles = "USER")
	void getMemberList_Fail_NotAdmin() throws Exception {
		/// given
		then(memberService).should(never()).getAdminMemberList(any(), any(), any());

		/// when & then
		mockMvc.perform(get("/api/v2/admin/members")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("인증되지 않은 사용자가 접근")
	void getMemberList_Fail_Unauthorized() throws Exception {
		/// given
		then(memberService).should(never()).getAdminMemberList(any(), any(), any());

		/// when & then
		mockMvc.perform(get("/api/v2/admin/members")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("역할 변경 - 성공 (USER -> BLACK)")
	@WithMockUser(roles = "ADMIN")
	void changeMemberRole_Success_UserToBlack() throws Exception {
		/// given
		Member updatedMember = Member.builder()
			.id(testMemberId)
			.nickname("testUser")
			.email("test@example.com")
			.role(MemberRole.BLACK)
			.build();

		given(memberService.updateMemberRole(testMemberId, MemberRole.BLACK))
			.willReturn(updatedMember);

		/// when & then
		mockMvc.perform(patch("/api/v2/admin/members/{memberId}", testMemberId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBodyRoleBlack))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("사용자 권한이 수정되었습니다."))
			.andExpect(jsonPath("$.data.memberId").value(testMemberId.toString()))
			.andExpect(jsonPath("$.data.nickname").value("testUser"))
			.andExpect(jsonPath("$.data.email").value("test@example.com"))
			.andExpect(jsonPath("$.data.role").value("BLACK"));
	}

	@Test
	@DisplayName("역할 변경 - 성공 (BLACK -> USER)")
	@WithMockUser(roles = "ADMIN")
	void changeMemberRole_Success_BlackToUser() throws Exception {
		/// given
		Member updatedMember = Member.builder()
			.id(testMemberId)
			.nickname("testUser")
			.email("test@example.com")
			.role(MemberRole.USER)
			.build();

		given(memberService.updateMemberRole(testMemberId, MemberRole.USER))
			.willReturn(updatedMember);

		/// when & then
		mockMvc.perform(patch("/api/v2/admin/members/{memberId}", testMemberId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBodyRoleUser))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("사용자 권한이 수정되었습니다."))
			.andExpect(jsonPath("$.data.memberId").value(testMemberId.toString()))
			.andExpect(jsonPath("$.data.nickname").value("testUser"))
			.andExpect(jsonPath("$.data.email").value("test@example.com"))
			.andExpect(jsonPath("$.data.role").value("USER"));
	}

	@Test
	@DisplayName("역할 변경 - 실패 존재하지 않는 회원에게 역할 변경 시도")
	@WithMockUser(roles = "ADMIN")
	void changeMemberRole_Fail_MemberNotFound() throws Exception {
		/// given
		// memberService.updateMemberRole 호출 시 ServiceException (404) 발생 모킹
		given(memberService.updateMemberRole(nonExistentMemberId, MemberRole.BLACK))
			.willThrow(new ServiceException("404", "존재하지 않는 사용자입니다."));

		/// when & then
		mockMvc.perform(patch("/api/v2/admin/members/{memberId}", nonExistentMemberId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBodyRoleBlack))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404"))
			.andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));

		then(memberService).should().updateMemberRole(eq(nonExistentMemberId), eq(MemberRole.BLACK));
	}

	@Test
	@DisplayName("역할 변경 - 실패 대상 회원이 ADMIN일 경우 역할 변경 시도")
	@WithMockUser(roles = "ADMIN")
	void changeMemberRole_Fail_TargetIsAdmin() throws Exception {
		/// given
		// memberService.updateMemberRole 호출 시 ServiceException (404) 발생 모킹
		given(memberService.updateMemberRole(adminMemberId, MemberRole.BLACK))
			.willThrow(new ServiceException("400", "관리자 역할은 변경할 수 없습니다."));

		/// when & then
		mockMvc.perform(patch("/api/v2/admin/members/{memberId}", adminMemberId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBodyRoleBlack))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("관리자 역할은 변경할 수 없습니다."));

		then(memberService).should().updateMemberRole(eq(adminMemberId), eq(MemberRole.BLACK));
	}

	@Test
	@DisplayName("역할 변경 - 실패 ADMIN 역할 부여 시도")
	@WithMockUser(roles = "ADMIN")
	void changeMemberRole_Fail_AssignAdminRole() throws Exception {
		/// given
		// memberService.updateMemberRole 호출 시 ServiceException (404) 발생 모킹
		given(memberService.updateMemberRole(testMemberId, MemberRole.ADMIN))
			.willThrow(new ServiceException("400", "관리자 역할은 부여할 수 없습니다."));

		/// when & then
		mockMvc.perform(patch("/api/v2/admin/members/{memberId}", testMemberId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBodyRoleAdmin))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("관리자 역할은 부여할 수 없습니다."));

		then(memberService).should().updateMemberRole(eq(testMemberId), eq(MemberRole.ADMIN));
	}

	@Test
	@DisplayName("역할 변경 - 실패 유효하지 않은 역할 문자열 전송 시 DTO 바인딩 실패 또는 검증 실패")
	@WithMockUser(roles = "ADMIN")
	void changeMemberRole_Fail_InvalidRoleString() throws Exception {
		/// when & then
		mockMvc.perform(patch("/api/v2/admin/members/{memberId}", testMemberId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBodyRoleInvalidRole))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("역할 변경 - 실패 ADMIN 권한 없는 사용자가 접근")
	@WithMockUser(roles = "USER")
	void changeMemberRole_Fail_NotAdmin() throws Exception {
		/// given
		// memberService.updateMemberRole 호출 시 ServiceException (404) 발생 모킹
		then(memberService).should(never()).updateMemberRole(any(), any());

		/// when & then
		mockMvc.perform(patch("/api/v2/admin/members/{memberId}", testMemberId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBodyRoleAdmin))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("역할 변경 - 실패 인증되지 않은 사용자가 접근")
	void changeMemberRole_Fail_Unauthorized() throws Exception {
		/// given
		// memberService.updateMemberRole 호출 시 ServiceException (404) 발생 모킹
		then(memberService).should(never()).updateMemberRole(any(), any());

		/// when & then
		mockMvc.perform(patch("/api/v2/admin/members/{memberId}", testMemberId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBodyRoleBlack))
			.andExpect(status().isUnauthorized());
	}
}