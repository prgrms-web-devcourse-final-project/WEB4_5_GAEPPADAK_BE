package site.kkokkio.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import site.kkokkio.global.security.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private final CustomUserDetailsService customUserDetailsService;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
			// CORS 설정 추가
			.cors(
				httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource()))

			// CSRF 보호 비활성화 (REST API + JWT 조합)
			.csrf(csrf -> csrf.disable())

			// HTTP 응답 헤더 설정
			.headers(headers -> headers
				// H2 콘솔 접근을 허용하기 위해 frameOptions 비활성화(dev 전용)
				.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
			)

			// 세션을 사용하지 않는 Stateless로 설정
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.authenticationProvider(authenticationProvider())

			// 엔드포인트별 권한 설정
			.authorizeHttpRequests(authorize ->
				authorize
					// 모든 HTTP 메소드에 대해 인증 없이 접근 가능한 경로
					.requestMatchers(getPublicEndpoints().toArray(String[]::new)
					).permitAll()

					// 댓글 GET 요청 허용
					.requestMatchers(HttpMethod.GET, "/api/v1/posts/*/comments")
					.permitAll()

					// 회원 권한
					.requestMatchers(getPubliCUserEndpoints().toArray(String[]::new)).hasRole("USER")
					// 관리자 권한
					.requestMatchers(getPublicAdminEndpoints().toArray(String[]::new)).hasRole("ADMIN")

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
			"https://login.aleph.kr",// Todo: 임시 프론트 배포 URL1
			"https://www.app4.qwas.shop", // Todo: 임시 프론트 배포 URL2
			"https://web.api.deploy.kkokkio.site:3000", // 프론트 API URL
			"http://localhost:3000", // 로컬용
			"https://api.deploy.kkokkio.site" // 백엔드 API URL
		)); // 프론트 사이트 추가
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 허용 HTTP 메소드
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

	// 패스워드 암호화를 위한 Bean
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// 모든 사용자
	private List<String> getPublicEndpoints() {
		return List.of(

			// root권한
			"/",

			// 로그인, 회원가입 등
			"/api/v1/auth/**",

			// Todo: 아래 엔드포인트는 개발 환경에서만, 운영 서버 반영 전 제거 필요

			// Swagger UI 관련 경로 허용
			"/swagger-ui/**",
			"/swagger-ui.html",
			"/swagger-resources/**",

			// OpenAPI 스펙
			"/v3/api-docs/**",
			"/webjars/**",

			// h2-console 확인
			"/h2-console/**"
		);
	}

	// USER(회원) 권한
	private List<String> getPubliCUserEndpoints() {
		return List.of(
			// 댓글 권한
			"/api/v1/posts/*/comments",
			"/api/v1/comments/*",
			"/api/v1/comments/*/like"
		);
	}

	// ADMIN(관리자) 권한
	private List<String> getPublicAdminEndpoints() {
		return List.of(
			// 댓글 권한
			"/api/v1/posts/*/comments",
			"/api/v1/comments/*",
			"/api/v1/comments/*/like"
		);
	}

}
