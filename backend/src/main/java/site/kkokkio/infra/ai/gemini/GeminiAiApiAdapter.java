package site.kkokkio.infra.ai.gemini;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import site.kkokkio.infra.ai.AiType;
import site.kkokkio.infra.ai.adapter.AiSummaryAdapter;
import site.kkokkio.infra.ai.gemini.dto.GeminiError;
import site.kkokkio.infra.ai.gemini.dto.GeminiErrorDetail;
import site.kkokkio.infra.ai.gemini.dto.GeminiErrorResponse;
import site.kkokkio.infra.ai.gemini.dto.GeminiResponse;
import site.kkokkio.infra.ai.prompt.AiSystemPromptResolver;
import site.kkokkio.infra.common.exception.ExternalApiErrorUtil;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiAiApiAdapter implements AiSummaryAdapter {

	@Qualifier("geminiWebClient")
	private final WebClient geminiWebClient;

	private final GeminiApiProperties props;
	private final AiSystemPromptResolver promptResolver;

	@Value("${mock.enabled:true}")
	private boolean mockEnabled;

	@Value("${mock.gemini-file:gemini-summary.json}")
	private String mockFile;

	@CircuitBreaker(name = "GEMINI_AI_CIRCUIT_BREAKER")
	@Retry(name = "GEMINI_AI_RETRY")
	@RateLimiter(name = "GEMINI_AI_RATE_LIMITER")
	@Override
	public CompletableFuture<String> summarize(AiType aiType, String content) {
		if (aiType != AiType.GEMINI) {
			throw new IllegalArgumentException("이 어댑터는 GEMINI 타입만 지원합니다.");
		}
		return summarize(content);
	}

	private CompletableFuture<String> summarize(String content) {
		if (mockEnabled) {
			return loadMockSummaryResponse();
		}

		// 시스템 프롬프트를 불러옴
		String systemPrompt = promptResolver.getPromptFor(AiType.GEMINI);

		Map<String, Object> body = Map.of(
			"model", props.getModel(),
			"messages", List.of(
				Map.of("role", "system", "content", systemPrompt),
				Map.of("role", "user", "content", content)
			)
		);

		return geminiWebClient.post()
			.uri("/chat/completions")
			.bodyValue(body)
			.retrieve()
			.onStatus(HttpStatusCode::isError, this::mapGeminiError)
			.bodyToMono(GeminiResponse.class)
			.map(response -> {
				if (response == null || response.getChoices().isEmpty()) {
					throw new IllegalStateException("Gemini 요약 응답이 없습니다.");
				}
				return response.getChoices().get(0).getMessage().getContent();
			})
			.toFuture();
	}

	private Mono<? extends Throwable> mapGeminiError(ClientResponse response) {
		return response.bodyToMono(GeminiErrorResponse.class)
			.defaultIfEmpty(new GeminiErrorResponse())
			.flatMap(err -> {
				GeminiError error = err.getError();
				String vendorCode = null;
				String vendorMsg = null;

				if (error != null) {
					vendorMsg = error.getMessage();
					if (error.getErrors() != null && !error.getErrors().isEmpty()) {
						GeminiErrorDetail detail = error.getErrors().getFirst();
						if (detail != null) {
							vendorCode = detail.getReason();
							if (detail.getMessage() != null && !detail.getMessage().isBlank()) {
								vendorMsg = detail.getMessage();
							}
						}
					}
				}

				return Mono.error(ExternalApiErrorUtil.of(response.statusCode(), vendorCode, vendorMsg));
			});
	}

	private CompletableFuture<String> loadMockSummaryResponse() {
		try (InputStream is = getClass().getResourceAsStream("/mock/" + mockFile)) {
			if (is == null) {
				throw new IOException("Mock 파일을 찾을 수 없습니다: " + mockFile);
			}
			ObjectMapper mapper = new ObjectMapper();
			GeminiResponse response = mapper.readValue(is, GeminiResponse.class);
			String content = response.getChoices().get(0).getMessage().getContent();
			return CompletableFuture.completedFuture(content);
		} catch (IOException e) {
			throw new RuntimeException("Gemini mock 파일 로딩 실패", e);
		}
	}
}
