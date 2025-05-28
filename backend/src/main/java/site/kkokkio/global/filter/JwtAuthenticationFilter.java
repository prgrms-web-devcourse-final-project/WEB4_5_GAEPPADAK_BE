package site.kkokkio.global.filter;

import java.io.IOException;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.CustomAuthException;
import site.kkokkio.global.util.JwtUtils;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;
	private final RedisTemplate<String, String> redisTemplate;

	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain) throws ServletException, IOException {

		// 쿠키에서 토큰 추출
		String token = jwtUtils.getJwtFromCookies(request).orElse(null);

		try {
			// 토큰 유효성 검사
			if (token != null && jwtUtils.isValidToken(token)) {
				// 블랙리스트 확인
				if (isTokenBlacklisted(token)) {
					setErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "로그아웃된 토큰입니다.");
					return;
				}

				// 정보 추출
				Claims claims = jwtUtils.getClaims(token);
				String email = claims.getSubject();
				String role = claims.get("role", String.class);
				Boolean isVerified = claims.get("isEmailVerified", Boolean.class);

				if (email != null && Boolean.TRUE.equals(isVerified)) {
					// UserDetails 객체 생성 (DB에서 사용자 정보 조회)
					UserDetails userDetails = new CustomUserDetails(email, role, isVerified);

					// UserDetails 기반으로 Authentication 객체 생성
					UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

					// SecurityContextHolder에 인증 정보 설정
					SecurityContextHolder.getContext().setAuthentication(authentication);
				} else {
					setErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "이메일 인증이 완료되지 않았습니다.");
					return;
				}
			}
		} catch (CustomAuthException e) {
			log.warn("토큰 에러 발생 : {}", e.getMessage());
			setErrorResponse(response,
				Integer.parseInt(e.getAuthErrorType().getHttpStatus()), e.getAuthErrorType().getDefaultMessage());
			return;
		} catch (ExpiredJwtException e) {
			log.warn("만료된 토큰 요청 발생: {}", e.getMessage());
			setErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "만료된 토큰입니다.");
			return;
		} catch (JwtException e) {
			log.warn("유효하지 않은 토큰 요청 발생: {}", e.getMessage());
			setErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "유효하지 않은 토큰입니다.");
			return;
		} catch (UsernameNotFoundException e) {
			log.warn("사용자를 찾지 못함 : {}", e.getMessage());
			setErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "사용자를 찾을 수 없습니다.");
			return;
		}
		// 검증 후 다음 필터로
		filterChain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getRequestURI().equals("/api/v1/auth/logout")
			|| request.getRequestURI().equals("/api/v1/auth/refresh")
			|| request.getRequestURI().equals("/api/v1/auth/login");
	}

	// Redis에서 토큰 블랙리스트 확인
	private boolean isTokenBlacklisted(String token) {
		return Boolean.TRUE.equals(redisTemplate.hasKey("blackList:" + token));
	}

	// 에러 핸들링
	private void setErrorResponse(HttpServletResponse response, Integer status, String message) throws IOException {
		response.setCharacterEncoding("UTF-8");
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
		RsData<Void> errorResponse = new RsData<>(status.toString(), message);
		response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
	}
}
