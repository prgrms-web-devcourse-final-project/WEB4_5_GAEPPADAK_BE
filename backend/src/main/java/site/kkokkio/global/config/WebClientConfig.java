package site.kkokkio.global.config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import site.kkokkio.infra.ai.gemini.GeminiProperties;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient naverWebClient(
            @Value("${naver.base-url}") String baseUrl) {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Youtube API 호출용 WebClient 빈 정의
     *
     * @param baseUrl application.yml에서 주입받는 Youtube API 기본 URL
     * @return Youtube API 호출용 WebClient
     */
    @Bean
    public WebClient youtubeWebClient(
            @Value("${youtube.api.base-url}") String baseUrl
    ) {
        return WebClient.builder()
            // YouTube API는 API 키를 주로 쿼리 파라미터로 사용하므로,
            // defaultHeader에 API 키를 추가하는 것보다 어댑터에서
            // 직접 쿼리 파라미터로 추가하는 방식이 더 일반적임
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

	@Bean
	@Qualifier("geminiWebClient")
	public WebClient geminiWebClient(
		GeminiProperties props
	) {
		return WebClient.builder()
			.baseUrl(props.getBaseUrl())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getKey())
			// openai 호환 api 방식으로는 필요 없으나 문제 발생시 추가 예정
			// .defaultHeader("x-goog-project-id", props.getProjectId())
			.build();
	}
}
