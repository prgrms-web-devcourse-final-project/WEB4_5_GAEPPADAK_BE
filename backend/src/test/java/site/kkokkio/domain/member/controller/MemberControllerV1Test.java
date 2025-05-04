package site.kkokkio.domain.member.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;
import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.service.AuthService;
import site.kkokkio.domain.member.service.MailService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.aspect.ResponseAspect;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.CustomAuthException;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(MemberControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ResponseAspect.class)               // ① AOP 빈 등록
@EnableAspectJAutoProxy(proxyTargetClass = true)
class MemberControllerV1Test {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberService memberService;

	@MockitoBean
	private JwtUtils jwtUtils;

	@MockitoBean
	private MailService mailService;

	@MockitoBean
	private AuthService authService;

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
				CustomAuthException.AuthErrorType.CREDENTIALS_MISMATCH,
				"인증 토큰이 없습니다."
			));

		// when & then
		mockMvc.perform(get("/api/v1/member/me")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status_code").value(401))
			.andExpect(jsonPath("$.err_code").value("CREDENTIALS_MISMATCH"))
			.andExpect(jsonPath("$.message").value("인증 토큰이 없습니다."));
	}
}