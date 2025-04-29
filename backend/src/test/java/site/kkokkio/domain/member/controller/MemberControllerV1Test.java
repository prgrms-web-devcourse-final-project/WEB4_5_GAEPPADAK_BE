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

import jakarta.servlet.http.HttpServletResponse;
import site.kkokkio.domain.member.controller.dto.MemberLoginResponse;
import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.service.MailService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.aspect.ResponseAspect;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.ServiceException;
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
		mockMvc.perform(post("/api/v1/auth/signup")
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
	@DisplayName("로그인 - 성공")
	void loginSuccess() throws Exception {
		// given
		MemberLoginResponse loginResponse = MemberLoginResponse.builder()
			.nickname("테스트쟁이")
			.email("test@test.com")
			.deleteAt(null)
			.role(MemberRole.USER)
			.token("tokenTest")
			.build();
		given(memberService.loginMember("test@test.com", "passHash"))
			.willReturn(loginResponse);
		willDoNothing().given(jwtUtils)
			.setJwtInCookie(eq("tokenTest"), any(HttpServletResponse.class));

		String loginJson = """
			{
			  "email":"test@test.com",
			  "passwordHash":"passHash"
			}
			""";

		// when & then
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("로그인 성공"))
			.andExpect(jsonPath("$.data.nickname").value("테스트쟁이"))
			.andExpect(jsonPath("$.data.email").value("test@test.com"))
			.andExpect(jsonPath("$.data.role").value("USER"));
	}

	@Test
	@DisplayName("로그인 - 실패 (인증 오류)")
	void loginFailure() throws Exception {
		// given
		given(memberService.loginMember(anyString(), anyString()))
			.willThrow(new ServiceException("401", "로그인에 실패했습니다."));

		String loginJson = """
			{
			  "email":"bad@test.com",
			  "passwordHash":"wrong"
			}
			""";

		// when & then
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("401"))
			.andExpect(jsonPath("$.message").value("로그인에 실패했습니다."));
	}

	@Test
	@DisplayName("이메일 인증 코드 전송 - 성공")
	void sendAuthCodeSuccess() throws Exception {
		// given
		given(mailService.sendAuthCode("test@example.com"))
			.willReturn(true);

		// when & then
		mockMvc.perform(post("/api/v1/auth/verify-email")
				.param("email", "test@example.com"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("인증 코드가 전송되었습니다."));
	}

	@Test
	@DisplayName("이메일 인증 코드 전송 - 실패")
	void sendAuthCodeFailure() throws Exception {
		// given
		given(mailService.sendAuthCode("test@example.com"))
			.willReturn(false);

		// when & then
		mockMvc.perform(post("/api/v1/auth/verify-email")
				.param("email", "test@example.com"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("500"))
			.andExpect(jsonPath("$.message").value("인증 코드 전송이 실패하였습니다."));
	}

	// 이메일 인증 검사는 실제 메일로 검사 필요

	// @Test
	// @DisplayName("이메일 인증 검사 - 성공")
	// void checkEmailSuccess() throws Exception {
	// 	// given
	// 	given(mailService.validationAuthCode(any(EmailVerificationRequest.class)))
	// 		.willReturn(true);
	//
	// 	String checkJson = """
	// 		{
	// 		  "email":"test@example.com",
	// 		  "code":"123456"
	// 		}
	// 		""";
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/auth/check-email")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(checkJson))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.code").value("200"))
	// 		.andExpect(jsonPath("$.message").value("이메일 인증에 성공하였습니다."));
	// }
	//
	// @Test
	// @DisplayName("이메일 인증 검사 - 실패")
	// void checkEmailFailure() throws Exception {
	// 	// given
	// 	given(mailService.validationAuthCode(any(EmailVerificationRequest.class)))
	// 		.willReturn(false);
	//
	// 	String checkJson = """
	// 		{
	// 		  "email":"test@example.com",
	// 		  "code":"000000"
	// 		}
	// 		""";
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/auth/check-email")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(checkJson))
	// 		.andExpect(status().isBadRequest())
	// 		.andExpect(jsonPath("$.code").value("400"))
	// 		.andExpect(jsonPath("$.message").value("이메일 인증에 실패하였습니다."));
	// }
}