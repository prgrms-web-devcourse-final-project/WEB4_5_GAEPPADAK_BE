package site.kkokkio.domain.member.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import site.kkokkio.domain.member.controller.dto.MemberLoginResponse;
import site.kkokkio.domain.member.service.AuthService;
import site.kkokkio.domain.member.service.MailService;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.global.aspect.ResponseAspect;
import site.kkokkio.global.auth.CustomUserDetailsService;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(AuthControllerV1.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import(ResponseAspect.class)               // ① AOP 빈 등록
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuthControllerV1Test {

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

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private RedisTemplate<String, String> redisTemplate;

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
		given(authService.login(eq("test@test.com"), eq("passHash"), any(HttpServletResponse.class)))
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
		given(authService.login(anyString(), anyString(), any(HttpServletResponse.class)))
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
	@DisplayName("토큰 재발급 - 성공")
	void refreshTokenSuccess() throws Exception {
		// given
		willCallRealMethod().given(jwtUtils).setJwtInCookie(anyString(), any(HttpServletResponse.class));

		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."));
	}

	@Test
	@DisplayName("로그아웃 - 성공")
	void logoutSuccess() throws Exception {
		// given
		willDoNothing().given(authService)
			.logout(any(HttpServletRequest.class), any(HttpServletResponse.class));

		// when & then
		mockMvc.perform(post("/api/v1/auth/logout"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("로그아웃 되었습니다."));
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

	@Test
	@DisplayName("이메일 인증 코드 전송 - 실패 (비회원)")
	void sendAuthCodeNonMemberFail() throws Exception {
		// given
		given(mailService.sendAuthCode("nonmember@example.com"))
			.willThrow(new ServiceException("404", "존재하지 않는 이메일입니다."));

		// when & then
		mockMvc.perform(post("/api/v1/auth/verify-email")
				.param("email", "nonmember@example.com"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404"))
			.andExpect(jsonPath("$.message").value("존재하지 않는 이메일입니다."));
	}
}
