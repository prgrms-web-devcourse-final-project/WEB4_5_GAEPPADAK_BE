package site.kkokkio.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import site.kkokkio.global.auth.CustomUserDetailsService;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.filter.JwtAuthenticationFilter;
import site.kkokkio.global.util.JwtUtils;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

	private final CustomUserDetailsService customUserDetailsService;
	private final RedisTemplate<String, String> redisTemplate;
	private final JwtUtils jwtUtils;
	private final ObjectMapper objectMapper;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
			// CORS 설정 추가
			.cors(
				httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource()))

			// CSRF 보호 비활성화 (REST API + JWT 조합)
			.csrf(AbstractHttpConfigurer::disable)

			// HTTP 응답 헤더 설정
			.headers(headers -> headers
				// H2 콘솔 접근을 허용하기 위해 frameOptions 비활성화(dev 전용)
				.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
			)

			// 세션을 사용하지 않는 Stateless로 설정
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)

			// 인증 제공자 설정
			.authenticationProvider(authenticationProvider())

			// JWT 인증 필터 추가
			.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

			// 로그인 관련 예외 처리
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(customAuthEntryPoint())
				.accessDeniedHandler(customAccessDeniedHandler())
			)

			// 엔드포인트별 권한 설정
			.authorizeHttpRequests(authorize ->
				authorize
					// — USER 로그인 필요
					.requestMatchers(HttpMethod.POST, "/api/v1/posts/*/comments").authenticated()        // 댓글 작성
					.requestMatchers(HttpMethod.POST, "/api/v1/auth/check-password").authenticated()    // 비밀번호 검증
					.requestMatchers(HttpMethod.PATCH, "/api/v1/member/me").authenticated()            // 회원 정보 수정

					.requestMatchers("/api/*/comments/**").authenticated()
					.requestMatchers("/api/*/reports/**").authenticated()
					.requestMatchers("/api/*/admin/**").authenticated()

					// 그 외 모든 요청 허용
					.anyRequest().permitAll()
			);

		return http.build();
	}

	// CORS 설정을 분리해 둔 Bean
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		// 모든 출처 허용 (운영 환경 배포 시 수정 필요)
		configuration.setAllowedOriginPatterns(Arrays.asList(
			// "https://login.aleph.kr", // 임시 프론트 배포 URL1
			// "https://www.app4.qwas.shop", // 임시 프론트 배포 URL2
			"https://web.api.deploy.kkokkio.site:3000", // 프론트 API URL
			"https://web.kkokkio.site", // 프론트 배포 URL
			"https://api.deploy.kkokkio.site", // 백엔드 dev API URL
			"https://api.prd.kkokkio.site"// 백엔드 prod API URL
		)); // 프론트 사이트 추가
		configuration.setAllowedMethods(
			Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")); // 허용 HTTP 메소드
		// configuration.setAllowedHeaders(Arrays.asList("*")); // HTTP 헤더(모두 허용)
		configuration.setAllowCredentials(true); // 쿠키 등 자격 증명
		// 클라이언트 노출 헤더
		configuration.setAllowedHeaders(List.of(
			"Authorization",
			"Content-Type",
			"X-Requested-With",
			"Accept",
			"Origin"
		));

		// 모든 엔드포인트에 대해 CORS 설정 적용
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	// DAO 기반 AuthenticationProvider 설정
	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(customUserDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	// AuthenticationManager를 AuthenticationConfiguration에서 가져오기
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	// JwtAuthenticationFilter Bean 등록
	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(jwtUtils, redisTemplate);
	}

	// 인증 안 된 상태로 보호된 엔드포인트에 접근했을 때 (401)
	@Bean
	public AuthenticationEntryPoint customAuthEntryPoint() {
		return (request, response, authException) -> {
			response.setCharacterEncoding("UTF-8");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

			RsData<Void> body = new RsData<>("401", "로그인이 필요합니다.");
			response.getWriter().write(objectMapper.writeValueAsString(body));
		};
	}

	// 권한이 부족한 상태로 엔드포인트에 접근했을 때 (403)
	@Bean
	public AccessDeniedHandler customAccessDeniedHandler() {
		return (request, response, accessDeniedException) -> {
			response.setCharacterEncoding("UTF-8");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

			RsData<Void> body = new RsData<>("403", "권한이 없습니다.");
			response.getWriter().write(objectMapper.writeValueAsString(body));
		};
	}

	// 패스워드 암호화를 위한 Bean
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
