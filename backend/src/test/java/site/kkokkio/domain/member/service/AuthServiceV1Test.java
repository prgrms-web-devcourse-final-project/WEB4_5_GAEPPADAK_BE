package site.kkokkio.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import site.kkokkio.domain.member.controller.dto.MemberLoginResponse;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.util.JwtUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceV1Test {

	@InjectMocks
	private AuthService authService;

	@Mock
	private MemberService memberService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtils jwtUtils;

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@BeforeEach
	void setUp() {
		// RedisTemplate의 opsForValue()가 반환할 mock 설정
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

	}

	@Test
	@DisplayName("로그인 성공")
	void login_success() {
		String email = "user@example.com";
		String rawPw = "plain";
		String encPw = "encoded";

		Member member = Member.builder()
			.email(email)
			.passwordHash(encPw)
			.nickname("nick")
			.role(MemberRole.USER)
			.build();

		// Redis TTL
		Duration rtTtl = Duration.ofMinutes(5);

		// Claims
		Map<String, Object> claims = Map.of(
			"isEmailVerified", true,
			"role", MemberRole.USER
		);

		member.setEmailVerified(true); // 테스트 통과를 위해 임시 이메일 인증

		// given
		given(memberService.findByEmail(email)).willReturn(member);
		given(passwordEncoder.matches(rawPw, encPw)).willReturn(true);
		given(jwtUtils.createToken(eq(email), anyMap())).willReturn("accessToken");
		given(jwtUtils.createRefreshToken(eq(email), anyMap())).willReturn("refreshToken");
		willDoNothing().given(valueOperations)
			.set(eq("refreshToken:" + email), eq("refreshToken"), any(Duration.class));

		// 쿠키 저장 stub
		willDoNothing().given(jwtUtils).setJwtInCookie(eq("accessToken"), any());
		willDoNothing().given(jwtUtils).setRefreshTokenInCookie(eq("refreshToken"), any());

		// when
		MemberLoginResponse result = authService.login(email, rawPw, response);

		// then
		assertThat(result.email()).isEqualTo(email);
		assertThat(result.token()).isEqualTo("accessToken");
		assertThat(result.refreshToken()).isEqualTo("refreshToken");
	}

	@Test
	@DisplayName("로그인 실패 - 비밀번호 틀림")
	void login_fail_passwordMismatch() {
		Member member = Member.builder()
			.email("user@example.com")
			.passwordHash("encoded")
			.build();

		given(memberService.findByEmail("user@example.com")).willReturn(member);
		given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

		assertThatThrownBy(() -> authService.login("user@example.com", "wrong", response))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("비밀번호가 올바르지 않습니다.");
	}

	@Test
	@DisplayName("토큰 재발급 성공")
	void refreshToken_success() {
		String email = "user@example.com";
		String rt = "refresh-token";

		Claims claims = mock(Claims.class);
		given(claims.getSubject()).willReturn(email);

		given(jwtUtils.getRefreshTokenFromCookies(request)).willReturn(Optional.of(rt));
		given(jwtUtils.getPayload(rt)).willReturn(claims);
		given(valueOperations.get("refreshToken:" + email)).willReturn(rt);
		given(jwtUtils.createToken(email, claims)).willReturn("new-access");
		willDoNothing().given(jwtUtils).setJwtInCookie(eq("new-access"), eq(response));

		// when
		authService.refreshToken(request, response);

		// then
		then(jwtUtils).should().setJwtInCookie("new-access", response);
	}

	@Test
	@DisplayName("로그아웃 성공")
	void logout_success() {
		String at = "access-token";
		String email = "user@example.com";

		Claims claims = mock(Claims.class);
		given(claims.get("email", String.class)).willReturn(email);

		given(jwtUtils.getJwtFromCookies(request)).willReturn(Optional.of(at));
		given(jwtUtils.getClaims(at)).willReturn(claims);
		given(jwtUtils.getPayload(at)).willReturn(claims);
		given(jwtUtils.getExpiration(at)).willReturn(new Date(System.currentTimeMillis() + 60000));

		willDoNothing().given(valueOperations)
			.set(eq("BL:" + at), eq("logout"), any(Duration.class));
		given(redisTemplate.delete("refreshToken:" + email)).willReturn(true);
		willDoNothing().given(jwtUtils).clearAuthCookies(response);

		// when & then
		assertThatCode(() -> authService.logout(request, response)).doesNotThrowAnyException();
	}

}
