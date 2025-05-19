package site.kkokkio.domain.member.controller;

import static org.hamcrest.text.StringContainsInOrder.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.controller.dto.MemberUpdateRequest;
import site.kkokkio.domain.member.controller.dto.PasswordResetRequest;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.service.AuthService;
import site.kkokkio.domain.member.service.MailService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.auth.AuthChecker;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.CustomUserDetailsService;
import site.kkokkio.global.config.SecurityConfig;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.CustomAuthException;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(MemberControllerV1.class)
@Import(SecurityConfig.class)
class MemberControllerV1Test {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	AuthChecker authChecker;

	@MockitoBean
	private MemberService memberService;

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

	@Test
	@DisplayName("회원가입 - 성공")
	void signupSuccess() throws Exception {
		// given
		MemberResponse memberResponse = new MemberResponse(
			"test@test.com",
			"테스트쟁이",
			LocalDate.of(1990, 1, 1),
			MemberRole.USER
		);

		given(memberService.createMember(any(MemberSignUpRequest.class)))
			.willReturn(memberResponse);

		String signupJson = """
			{
			  "email":"test@test.com",
			  "nickname":"테스트쟁이",
			  "passwordHash":"password123!",
			  "birthDate": "1990-01-01"
			}
			""";

		// when & then
		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
			.andExpect(jsonPath("$.data.nickname").value("테스트쟁이"))
			.andExpect(jsonPath("$.data.birthDate").value("1990-01-01"))
			.andExpect(jsonPath("$.data.email").value("test@test.com"))
			.andExpect(jsonPath("$.data.role").value("USER"));
	}

	@Test
	@DisplayName("회원 정보 조회 - 성공")
	void getMember_success() throws Exception {
		// given
		MemberResponse respDto = new MemberResponse(
			"user@example.com",
			"tester",
			LocalDate.of(1990, 1, 1),
			MemberRole.USER
		);
		given(memberService.getMemberInfo(eq("user@example.com")))
			.willReturn(respDto);

		Member member = Member.builder()
			.email("user@example.com")
			.role(MemberRole.USER)
			.build();

		// when & then
		mockMvc.perform(get("/api/v1/members/me")
				.with(user(new CustomUserDetails(member.getEmail(), member.getRole().toString(), true)))
				.with(csrf())
				.contentType(APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("마이페이지 조회 성공"))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("tester"))
			.andExpect(jsonPath("$.data.birthDate").value("1990-01-01"))
			.andExpect(jsonPath("$.data.role").value("USER"));
	}

	@Test
	@DisplayName("회원 정보 조회 - 인증 토큰 누락으로 실패")
	void getMember_noToken_fail() throws Exception {
		// given: 토큰 누락 시 CustomAuthException 발생
		given(memberService.getMemberInfo(any(String.class)))
			.willThrow(new CustomAuthException(
				CustomAuthException.AuthErrorType.MISSING_TOKEN));

		Member member = Member.builder()
			.email("user@example.com")
			.role(MemberRole.USER)
			.build();

		// when & then
		mockMvc.perform(get("/api/v1/members/me")
				.with(user(new CustomUserDetails(member.getEmail(), member.getRole().toString(), true)))
				.with(csrf())
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(401))
			.andExpect(jsonPath("$.message").value("인증 토큰이 없어 인증 실패"));
	}

	@Test
	@DisplayName("회원 정보 수정 성공")
	void modifyMember_success() throws Exception {
		// given
		MemberUpdateRequest request = new MemberUpdateRequest("ps123123!", "after");
		MemberResponse expectedResponse = new MemberResponse(
			"user@example.com",
			request.nickname(),
			LocalDate.of(1990, 1, 1),
			MemberRole.USER
		);
		given(memberService.modifyMemberInfo(any(CustomUserDetails.class), any(MemberUpdateRequest.class)))
			.willReturn(expectedResponse);

		Member member = Member.builder()
			.id(UUID.randomUUID())
			.email("user@example.com")
			.nickname("currentNickname")
			.passwordHash("encodedPassword")
			.birthDate(LocalDate.of(1990, 1, 1))
			.role(MemberRole.USER)
			.emailVerified(true)
			.build();

		String updateJson = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(patch("/api/v1/members/me")
				.with(user(new CustomUserDetails(
					member.getEmail(), member.getRole().toString(), member.isEmailVerified())))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("회원정보가 정상적으로 수정되었습니다."));
	}

	@Test
	@DisplayName("회원 정보 수정 실패 - 유효성")
	void modifyMember_Validation_failed() throws Exception {
		// given
		MemberUpdateRequest request = new MemberUpdateRequest("ps", "veryverylongNickname");
		MemberResponse memberResponse = new MemberResponse(
			"user@example.com",
			request.nickname(), // 요청 닉네임 사용
			LocalDate.of(1990, 1, 1),
			MemberRole.USER
		);
		given(memberService.modifyMemberInfo(any(CustomUserDetails.class), any(MemberUpdateRequest.class)))
			.willReturn(memberResponse);

		Member member = Member.builder()
			.id(UUID.randomUUID())
			.email("user@example.com")
			.nickname("currentNickname")
			.passwordHash("encodedPassword")
			.birthDate(LocalDate.of(1990, 1, 1))
			.role(MemberRole.USER)
			.emailVerified(true)
			.build();

		String updateJson = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(patch("/api/v1/members/me")
				.with(user(new CustomUserDetails(
					member.getEmail(), member.getRole().toString(), member.isEmailVerified())))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateJson))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message", stringContainsInOrder(
				"nickname : Size : 닉네임은 2~10자 사이여야 합니다.\n",
				"password : Pattern : 비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.\n",
				"password : Size : 비밀번호는 8~20자 사이여야 합니다."
			)));
	}

	@Test
	@DisplayName("회원 탈퇴 - 성공")
	void deleteMember_success() throws Exception {
		// given
		Member member = Member.builder()
			.id(UUID.randomUUID())
			.email("user@example.com")
			.nickname("currentNickname")
			.role(MemberRole.USER)
			.build();

		// when & then
		mockMvc.perform(delete("/api/v1/members/me")
				.with(user(new CustomUserDetails(member.getEmail(), member.getRole().toString(), true)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("회원이 삭제 되었습니다."));
	}

	@Test
	@DisplayName("비밀번호 초기화 - 성공")
	void resetPassword_success() throws Exception {
		// given
		PasswordResetRequest req = new PasswordResetRequest(
			"user@example.com",
			"newPass123!"
		);
		doNothing().when(memberService).resetPassword(any(PasswordResetRequest.class));
		String json = objectMapper.writeValueAsString(req);

		// when & then
		mockMvc.perform(patch("/api/v1/members/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));
	}

	@Test
	@DisplayName("비밀번호 초기화 - 인증 미완료")
	void resetPassword_notVerified_fail() throws Exception {
		// given
		PasswordResetRequest req = new PasswordResetRequest(
			"user@example.com",
			"newPass123!"
		);
		doThrow(new ServiceException("401", "인증코드가 유효하지 않습니다."))
			.when(memberService).resetPassword(any(PasswordResetRequest.class));
		String json = objectMapper.writeValueAsString(req);

		// when & then
		mockMvc.perform(patch("/api/v1/members/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(401))
			.andExpect(jsonPath("$.message").value("인증코드가 유효하지 않습니다."));
	}

	@Test
	@DisplayName("비밀번호 초기화 - 이메일 없음")
	void resetPassword_emailNotFound_fail() throws Exception {
		// given
		PasswordResetRequest req = new PasswordResetRequest(
			"unknown@example.com",
			"newPass123!"
		);
		doThrow(new ServiceException("404", "존재하지 않는 이메일입니다."))
			.when(memberService).resetPassword(any(PasswordResetRequest.class));
		String json = objectMapper.writeValueAsString(req);

		// when & then
		mockMvc.perform(patch("/api/v1/members/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(404))
			.andExpect(jsonPath("$.message").value("존재하지 않는 이메일입니다."));
	}
}
