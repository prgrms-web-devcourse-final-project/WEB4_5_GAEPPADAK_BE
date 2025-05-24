package site.kkokkio.domain.member.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.member.controller.dto.MemberLoginResponse;
import site.kkokkio.domain.member.controller.dto.PasswordVerificationRequest;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.exception.CustomAuthException;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.util.JwtUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final MemberService memberService;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtils jwtUtils;
	private final RedisTemplate<String, String> redisTemplate;

	/** 로그인 -> Access/Refresh 토큰 생성 + Redis 저장 + 쿠키 세팅 */
	@Transactional
	public MemberLoginResponse login(String email, String rawPassword, HttpServletResponse response) {
		Member member = memberService.findByEmail(email);

		// 비밀번호 확인
		if (!passwordEncoder.matches(rawPassword, member.getPasswordHash())) {
			throw new ServiceException("401", "비밀번호가 올바르지 않습니다.");
		}
		// 인증 확인
		if (!member.isEmailVerified()) {
			throw new ServiceException("403", "인증되지 않은 회원입니다.");
		}

		Map<String, Object> claims = new HashMap<>();
		claims.put("isEmailVerified", member.isEmailVerified());
		claims.put("role", member.getRole());

		// token, refreshToken 생성
		String accessToken = jwtUtils.createToken(member.getEmail(), claims);
		String refreshToken = jwtUtils.createRefreshToken(member.getEmail(), claims);

		// Redis에 Refresh Token 저장 (키: "refreshToken:<email>")
		Duration rtTtl = Duration.ofMillis(jwtUtils.getRefreshTokenExpiration());
		redisTemplate.opsForValue()
			.set("refreshToken:" + member.getEmail(), refreshToken, rtTtl);

		// 쿠키에 토큰 세팅
		jwtUtils.setJwtInCookie(accessToken, response);
		jwtUtils.setRefreshTokenInCookie(refreshToken, response);

		return MemberLoginResponse.of(member, accessToken, refreshToken);
	}

	/** Refresh Token 검증 후 Access/Refresh 재발급 + Redis 갱신 + 쿠키 업데이트 */
	@Transactional
	public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
		String rt = jwtUtils.getRefreshTokenFromCookies(request)
			.orElseThrow(() -> new ServiceException("401", "리프레시 토큰이 없습니다."));

		// 페이로드에서 이메일 추출
		String email = jwtUtils.getPayload(rt).getSubject();

		// Redis에 저장된 refreshToken와 비교
		String savedRt = redisTemplate.opsForValue().get("refreshToken:" + email);
		if (savedRt == null || !savedRt.equals(rt)) {
			throw new ServiceException("401", "유효하지 않은 리프레시 토큰입니다.");
		}

		// 새로운 토큰 발급
		Map<String, Object> claims = jwtUtils.getPayload(rt);
		String newAt = jwtUtils.createToken(email, claims);

		// 쿠키에도 업데이트
		jwtUtils.setJwtInCookie(newAt, response);
	}

	/** 로그아웃 -> Access Token 블랙리스트 등록 + Refresh Token 삭제 + 쿠키 삭제 */
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		Optional<String> token = jwtUtils.getJwtFromCookies(request);

		if (token.isPresent()) {
			String accessToken = token.get();

			try {
				String email = jwtUtils.getClaims(accessToken).getSubject();

				long remainingMs = jwtUtils.getExpiration(accessToken).getTime() - System.currentTimeMillis();
				redisTemplate.opsForValue()
					.set("blackList:" + accessToken, "logout", Duration.ofMillis(remainingMs));

				redisTemplate.delete("refreshToken:" + email);
			} catch (JwtException | IllegalArgumentException | CustomAuthException e) {
				log.info(e.toString());
			}
		}
		jwtUtils.clearAuthCookies(response);
	}

	public boolean checkPassword(PasswordVerificationRequest request, CustomUserDetails userDetails) {
		Member member = memberService.findByEmail(userDetails.getUsername());
		return passwordEncoder.matches(request.getPassword(), member.getPasswordHash());
	}
}
