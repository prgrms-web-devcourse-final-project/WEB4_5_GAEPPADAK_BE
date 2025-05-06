package site.kkokkio.global.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import site.kkokkio.global.security.CustomUserDetailsService;
import site.kkokkio.global.util.JwtUtils;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;
	private final CustomUserDetailsService userDetailsService;
	private final RedisTemplate<String, String> redisTemplate;

	// 필터 체인 통과
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		String method = request.getMethod();

		// Auth 관련 엔드포인트 ⇒ 무조건 스킵
		if (List.of(
			"/api/v1/auth/login",
			"/api/v1/auth/signup",
			"/api/v1/auth/verify-email",
			"/api/v1/auth/check-email",
			"/api/v1/auth/refresh"
		).contains(path)) {
			return true;
		}

		// “댓글 쓰기·수정·삭제·좋아요” 만 필터 적용
		boolean isCommentWriteEndpoint =
			("POST".equals(method) && path.matches("^/api/v1/posts/\\d+/comments$"))            // 댓글 작성
				|| ("PATCH".equals(method) && path.matches("^/api/v1/comments/\\d+$"))          // 댓글 수정
				|| ("DELETE".equals(method) && path.matches("^/api/v1/comments/\\d+$"))         // 댓글 삭제
				|| ("POST".equals(method) && path.matches("^/api/v1/comments/\\d+/like$"))	  // 댓글 좋아요
				|| ("DELETE".equals(method) && path.matches("^/api/v1/comments/\\d+/like$"));	  // 댓글 좋아요 취소

		// isCommentWriteEndpoint 이면 필터 동작(= false 리턴), 아니면 스킵(= true 리턴)
		return !isCommentWriteEndpoint;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		// 헤더에서 토큰 추출
		String headerToken = extractTokenFromHeader(request);

		// 쿠키에서 토큰 추출
		String cookieToken = extractTokenFromCookie(request);

		// 둘 중 하나라도 있으면 있증 처리
		String token = headerToken != null ? headerToken : cookieToken;

		if (token != null) {
			try {
				// 블랙리스트 확인
				if (isTokenBlacklisted(token)) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그아웃된 토큰입니다.");
					return;
				}

				Claims claims = jwtUtils.getClaims(token);
				String email = claims.getSubject();
				String role = claims.get("role", String.class);
				Boolean isVerified = claims.get("isEmailVerified", Boolean.class);

				if (email != null && Boolean.TRUE.equals(isVerified)) {
					// UserDetails 객체 생성 (DB에서 사용자 정보 조회)
					UserDetails userDetails = userDetailsService.loadUserByUsername(email);
					List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

					// UserDetails 기반으로 Authentication 객체 생성
					UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

					// SecurityContextHolder에 인증 정보 설정
					SecurityContextHolder.getContext().setAuthentication(authentication);
				} else {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "이메일 인증이 완료되지 않았습니다.");
					return;
				}
			} catch (ExpiredJwtException e) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "토큰이 만료되었습니다.");
				return;
			} catch (JwtException e) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰입니다.");
				return;
			}
		}
		// 검증 후 다음 필터로
		filterChain.doFilter(request, response);
	}

	// 헤더에서 토큰 추출하는 메서드
	private String extractTokenFromHeader(HttpServletRequest request) {
		String beareToken = request.getHeader("Authorization");
		if (beareToken != null && beareToken.startsWith("Bearer ")) {
			return beareToken.substring(7);
		}
		return null;
	}

	// 쿠키에서 토큰 추출하는 메서드
	private String extractTokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("accessToken".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	// Redis에서 토큰 블랙리스트 확인
	private boolean isTokenBlacklisted(String token) {
		return Boolean.TRUE.equals(redisTemplate.hasKey("blackList:" + token));
	}

	// Redis에 저장된 액세스 토큰과 일치하는지 확인
	private boolean isTokenInRedis(String email, String token) {
		String storedToken = redisTemplate.opsForValue().get("accessToken:" + email);
		return token.equals(storedToken);
	}

}


