package site.kkokkio.domain.member.controller;

import static org.hamcrest.text.StringContainsInOrder.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.controller.dto.MemberUpdateRequest;
import site.kkokkio.domain.member.service.AuthService;
import site.kkokkio.domain.member.service.MailService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.aspect.ResponseAspect;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.CustomAuthException;
import site.kkokkio.global.security.CustomUserDetailsService;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(MemberControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ResponseAspect.class)               // ① AOP 빈 등록
@EnableAspectJAutoProxy(proxyTargetClass = true)
class MemberControllerV1Test {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;


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
		mockMvc.perform(post("/api/v1/member/signup")
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
		given(memberService.getMemberInfo(any(HttpServletRequest.class)))
			.willReturn(respDto);

		// when & then
		mockMvc.perform(get("/api/v1/member/me")
				.accept(MediaType.APPLICATION_JSON))
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
		given(memberService.getMemberInfo(any(HttpServletRequest.class)))
			.willThrow(new CustomAuthException(
				CustomAuthException.AuthErrorType.MISSING_TOKEN));

		// when & then
		mockMvc.perform(get("/api/v1/member/me")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(401))
			.andExpect(jsonPath("$.message").value("인증 토큰이 없어 인증 실패"));
	}

	@Test
	@DisplayName("회원 정보 수정 성공")
	void modifyMember_success() throws Exception {
		// given
		MemberUpdateRequest request = new MemberUpdateRequest( "ps123123!","after"); // 요청 객체 먼저 생성
		MemberResponse memberResponse = new MemberResponse(
			"user@example.com",
			request.nickname(), // 요청 닉네임 사용
			LocalDate.of(1990, 1, 1),
			MemberRole.USER
		);
		given(memberService.modifyMemberInfo(any(HttpServletRequest.class), any(MemberUpdateRequest.class)))
			.willReturn(memberResponse);

		HttpServletRequest req = mock(HttpServletRequest.class);
		given(jwtUtils.getJwtFromCookies(req)).willReturn(Optional.of("valid.token"));
		given(jwtUtils.isValidToken("valid.token")).willReturn(true);

		String updateJson = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(patch("/api/v1/member/me")
				.cookie(new Cookie("access-token", "valid.token"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("회원정보가 정상적으로 수정되었습니다."))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("after")) // "after"로 검증
			.andExpect(jsonPath("$.data.birthDate").value("1990-01-01"))
			.andExpect(jsonPath("$.data.role").value("USER"));
	}

	@Test
	@DisplayName("회원 정보 수정 실패 - 유효성")
	void modifyMember_Validation_failed() throws Exception {
		// given
		MemberUpdateRequest request = new MemberUpdateRequest( "ps","veryverylongNickname"); // 요청 객체 먼저 생성
		MemberResponse memberResponse = new MemberResponse(
			"user@example.com",
			request.nickname(), // 요청 닉네임 사용
			LocalDate.of(1990, 1, 1),
			MemberRole.USER
		);
		given(memberService.modifyMemberInfo(any(HttpServletRequest.class), any(MemberUpdateRequest.class)))
			.willReturn(memberResponse);

		HttpServletRequest req = mock(HttpServletRequest.class);
		given(jwtUtils.getJwtFromCookies(req)).willReturn(Optional.of("valid.token"));
		given(jwtUtils.isValidToken("valid.token")).willReturn(true);

		String updateJson = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(patch("/api/v1/member/me")
				.cookie(new Cookie("access-token", "valid.token"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateJson))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message", stringContainsInOrder(
				"nickname : Size : 닉네임은 2~10자 사이여야 합니다.",
				"passwordHash : Pattern : 비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.",
				"passwordHash : Size : 비밀번호는 8~20자 사이여야 합니다."
			)));
	}

	@Test
	@DisplayName("회원 정보 수정 - 인증 토큰 누락으로 실패")
	void modifyMember_noToken_fail() throws Exception {
		// given: 토큰 누락 시 CustomAuthException 발생
		given(memberService.modifyMemberInfo(any(HttpServletRequest.class), any(MemberUpdateRequest.class)))
			.willThrow(new CustomAuthException(
				CustomAuthException.AuthErrorType.MISSING_TOKEN));

		// when & then
		mockMvc.perform(patch("/api/v1/member/me")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(401))
			.andExpect(jsonPath("$.message").value("인증 토큰이 없어 인증 실패"));
	}
}