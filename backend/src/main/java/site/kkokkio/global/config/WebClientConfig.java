package site.kkokkio.global.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.netty.http.client.HttpClient;
import site.kkokkio.infra.ai.claude.ClaudeApiProperties;
import site.kkokkio.infra.ai.gemini.GeminiApiProperties;
import site.kkokkio.infra.ai.gpt.GptApiProperties;

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
		GeminiApiProperties props
	) {
		// WebClient 전용 ObjectMapper
		ObjectMapper geminiMapper = new ObjectMapper()
			// unquoted control chars 허용
			.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		MediaType jsonUtf8 = MediaType.parseMediaType("application/json; charset=UTF-8");

		return WebClient.builder()
			.baseUrl(props.getBaseUrl())
			// 요청 본문 Content-Type
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
			// 응답으로 JSON만 받겠다고 명시
			.defaultHeader(HttpHeaders.ACCEPT, "application/json; charset=UTF-8")
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getKey())
			.codecs(spec -> {
				spec.defaultCodecs().jackson2JsonDecoder(
					new Jackson2JsonDecoder(geminiMapper, jsonUtf8)
				);
				spec.defaultCodecs().jackson2JsonEncoder(
					new Jackson2JsonEncoder(geminiMapper, jsonUtf8)
				);
			})
			.clientConnector(new ReactorClientHttpConnector(
				HttpClient.create()
					.responseTimeout(Duration.ofSeconds(5))
			))
			.build();
	}

	@Bean
	@Qualifier("gptWebClient")
	public WebClient gptWebClient(
		GptApiProperties props
	) {
		// WebClient 전용 ObjectMapper
		ObjectMapper gptMapper = new ObjectMapper()
			// unquoted control chars 허용
			.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		MediaType jsonUtf8 = MediaType.parseMediaType("application/json; charset=UTF-8");

		return WebClient.builder()
			.baseUrl(props.getBaseUrl())
			// 요청 본문 Content-Type
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
			// 응답으로 JSON만 받겠다고 명시
			.defaultHeader(HttpHeaders.ACCEPT, "application/json; charset=UTF-8")
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getKey())
			.codecs(spec -> {
				spec.defaultCodecs().jackson2JsonDecoder(
					new Jackson2JsonDecoder(gptMapper, jsonUtf8)
				);
				spec.defaultCodecs().jackson2JsonEncoder(
					new Jackson2JsonEncoder(gptMapper, jsonUtf8)
				);
			})
			.clientConnector(new ReactorClientHttpConnector(
				HttpClient.create()
					.responseTimeout(Duration.ofSeconds(5))
			))
			.build();
	}

	@Bean
	@Qualifier("claudeWebClient")
	public WebClient claudeWebClient(
		ClaudeApiProperties props
	) {
		// WebClient 전용 ObjectMapper
		ObjectMapper claudeMapper = new ObjectMapper()
			// unquoted control chars 허용
			.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		MediaType jsonUtf8 = MediaType.parseMediaType("application/json; charset=UTF-8");

		return WebClient.builder()
			.baseUrl(props.getBaseUrl())
			// 요청 본문 Content-Type
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
			// 응답으로 JSON만 받겠다고 명시
			.defaultHeader(HttpHeaders.ACCEPT, "application/json; charset=UTF-8")
			.defaultHeader("x-api-key", props.getKey())
			.defaultHeader("anthropic-version", "2023-06-01")
			.codecs(spec -> {
				spec.defaultCodecs().jackson2JsonDecoder(
					new Jackson2JsonDecoder(claudeMapper, jsonUtf8)
				);
				spec.defaultCodecs().jackson2JsonEncoder(
					new Jackson2JsonEncoder(claudeMapper, jsonUtf8)
				);
			})
			.clientConnector(new ReactorClientHttpConnector(
				HttpClient.create()
					.responseTimeout(Duration.ofSeconds(5))
			))
			.build();
	}
}
