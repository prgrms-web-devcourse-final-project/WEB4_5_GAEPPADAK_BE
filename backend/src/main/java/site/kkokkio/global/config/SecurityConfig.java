package site.kkokkio.global.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

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

			// 엔드포인트별 권한 설정
			.authorizeHttpRequests(authorize -> authorize
				// 인증 없이 접근 허용할 엔드포인트
				// .requestMatchers(
				// 	"/api/auth/**",        // 로그인·회원가입 등
				// 	"/swagger-ui/**",      // Swagger UI
				// 	"/v3/api-docs/**"      // OpenAPI 스펙
				// ).permitAll()

				// 그 외 모든 요청은 인증 필요
				// .anyRequest().authenticated()

				// 개발을 위해 모두 오픈(운영 배포시 변경 예정)
				.anyRequest().permitAll()
			);

		return http.build();
	}

	// CORS 설정을 분리해 둔 Bean
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		// 모든 출처 허용 (운영 환경 배포 시 수정 필요)
		configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000")); // 프론트 운영 주소로 변경 필요
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 허용 HTTP 메소드
		configuration.setAllowedHeaders(Arrays.asList("*")); // HTTP 헤더(모두 허용)
		configuration.setAllowCredentials(true); // 쿠키 등 자격 증명
		configuration.addExposedHeader("Authorization"); // 클라이언트 노출 헤더

		// 모든 엔드포인트에 대해 CORS 설정 적용
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	// 패스워드 암호화를 위한 Bean
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
