package site.kkokkio.global.util;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.global.exception.CustomAuthException;

// JWT 관련 유틸리티 클래스
@Slf4j
@Component
public class JwtUtils {

	@Value("${spring.jwt.secret-key}")
	private String secretKey;

	@Value("${spring.jwt.expiration}")
	private Long expiration;// dev 환경 10분

	private final Long refreshTokenExpiration = 7 * 24 * 60 * 60 * 1000L; // 리프레시 토큰 만료일 7일

	// token 만료 시간 반환
	public long getAccessTokenExpiration() {
		return expiration;
	}

	// Claims 호출
	public Claims getClaims(String token) {
		SecretKey key = getSecretKey();
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	public Date getExpiration(String token) {
		try {
			SecretKey key = getSecretKey();
			return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getExpiration();
		} catch (JwtException | IllegalArgumentException e) {
			handleAuthException(e);
		}
		return null;
	}

	// Refresh Token 만료 시간 반환
	public long getRefreshTokenExpiration() {
		return refreshTokenExpiration;
	}

	// JWT 생성
	public String createToken(String email, Map<String, Object> claims) {
		SecretKey key = getSecretKey();
		Date issuedAt = new Date();

		return Jwts.builder()
			.subject(email) // 사용자 식별자(email)
			.claims(claims) // 사용자 정보 포함
			.issuedAt(issuedAt) // 발급 시간
			.expiration(new Date(issuedAt.getTime() + expiration)) // 만료 시간
			.signWith(key) // 알고리즘 자동 인식 (HS256)
			.compact();
	}

	// 리프레시 토큰 생성
	public String createRefreshToken(String email, Map<String, Object> claims) {
		SecretKey key = getSecretKey();
		Date issuedAt = new Date();

		return Jwts.builder()
			.subject(email)
			.claims(claims)
			.issuedAt(issuedAt)
			.expiration(new Date(issuedAt.getTime() + refreshTokenExpiration))
			.signWith(key)
			.compact();
	}

	// JWT 유효성 검사 <- 토큰이 유효하지 않으면 CustomAuthException을 던진다.
	public boolean isValidToken(String token) {
		SecretKey key = getSecretKey();
		try {
			Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token);
			return true;
		} catch (ExpiredJwtException | MalformedJwtException | IllegalArgumentException |
				 UnsupportedJwtException | SecurityException e) {
			handleAuthException(e);
			return false;
		}
	}

	// JWT 페이로드 추출
	public Claims getPayload(String token) {
		try {
			SecretKey key = getSecretKey();
			return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload(); // Claims 객체 직접 반환
		} catch (JwtException | IllegalArgumentException e) {
			handleAuthException(e);
			return null;
		}
	}

	//JWT -> 쿠키에 저장
	public void setJwtInCookie(String token, HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from("accessToken", token)
			.httpOnly(true) // 자바스크립트 접근 차단 (XSS 방지)
			.path("/") // 전체 사이트에서 접근 가능

			// Todo: 프론트, 백엔드 도메인 일치 후(Strict)적용
			.sameSite("None") // 외부 사이트 요청 차단 (CSRF 방지)
			.maxAge(Duration.ofMillis(expiration)) // Access Token 만료 시간 : 10분
			.secure(true) // HTTPS 통신 시에만 전송
			.build();

		response.addHeader("Set-Cookie", cookie.toString());
		log.info("cookie = {}", cookie.toString());
	}

	// 리프레시 토큰을 쿠키에 저장
	public void setRefreshTokenInCookie(String token, HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
			.httpOnly(true)     // 자바스크립트 접근 차단 (XSS 방지)
			.path("/")      // 인증 경로에서만 접근 가능

			// Todo: 프론트, 백엔드 도메인 일치 후(Strict)적용
			.sameSite("None")   // 외부 사이트 요청 허용 (CORS 환경 대응)
			.maxAge(Duration.ofMillis(refreshTokenExpiration)) // Refresh Token 만료 시간 : 7일
			.secure(true)       // HTTPS 통신 시에만 전송
			.build();

		response.addHeader("Set-Cookie", cookie.toString());
		log.info("Refresh token cookie = {}", cookie.toString());
	}

	// 쿠키에서 JWT 추출
	public Optional<String> getJwtFromCookies(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();

		if (cookies == null || cookies.length == 0) { // 쿠키가 없을 경우
			return Optional.empty();
		}

		Arrays.stream(cookies)
			.forEach(cookie -> log.info("Cookie Name: {}, Value: {}", cookie.getName(), cookie.getValue()));

		return Arrays.stream(cookies)
			.filter(cookie -> "accessToken".equals(cookie.getName()))
			.map(Cookie::getValue)
			.findFirst();
	}

	// 쿠키에서 리프레시 토큰 추출
	public Optional<String> getRefreshTokenFromCookies(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();

		if (cookies == null || cookies.length == 0) {
			return Optional.empty();
		}

		return Arrays.stream(cookies)
			.filter(cookie -> "refreshToken".equals(cookie.getName()))
			.map(Cookie::getValue)
			.findFirst();
	}

	// 쿠키 삭제 (로그아웃 시 사용)
	public void clearAuthCookies(HttpServletResponse response) {
		// 액세스 토큰 쿠키 삭제
		ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
			.httpOnly(true)
			.path("/")
			.maxAge(0) // 즉시 만료
			.sameSite("None")
			.secure(true)
			.build();

		// 리프레시 토큰 쿠키 삭제
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
			.httpOnly(true)
			.path("/auth")
			.maxAge(0) // 즉시 만료
			.sameSite("None")
			.secure(true)
			.build();

		response.addHeader("Set-Cookie", accessCookie.toString());
		response.addHeader("Set-Cookie", refreshCookie.toString());
	}

	// 시크릿 키 생성
	private SecretKey getSecretKey() {
		SecretKey key = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secretKey)); // 16진수 문자열 → 바이트 배열
		return key;
	}

	// JWT 레이어 예외 처리 중앙화
	private void handleAuthException(Exception e) {
		if (e instanceof ExpiredJwtException) {
			throw new CustomAuthException(CustomAuthException.AuthErrorType.TOKEN_EXPIRED);
		} else if (e instanceof MalformedJwtException || e instanceof IllegalArgumentException) {
			throw new CustomAuthException(CustomAuthException.AuthErrorType.MALFORMED_TOKEN);
		} else if (e instanceof UnsupportedJwtException) {
			throw new CustomAuthException(CustomAuthException.AuthErrorType.UNSUPPORTED_TOKEN);
		} else if (e instanceof SecurityException) {
			throw new CustomAuthException(CustomAuthException.AuthErrorType.CREDENTIALS_MISMATCH);
		}
	}
}