package site.kkokkio.infra.ai.claude;

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
import site.kkokkio.domain.post.port.out.AiSummaryPort;
import site.kkokkio.infra.ai.AiType;
import site.kkokkio.infra.ai.claude.dto.ClaudeError;
import site.kkokkio.infra.ai.claude.dto.ClaudeErrorDetail;
import site.kkokkio.infra.ai.claude.dto.ClaudeErrorResponse;
import site.kkokkio.infra.ai.claude.dto.ClaudeResponse;
import site.kkokkio.infra.ai.prompt.AiSystemPromptResolver;
import site.kkokkio.infra.common.exception.ExternalApiErrorUtil;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeAiApiPort implements AiSummaryPort {

	@Qualifier("claudeWebClient")
	private final WebClient claudeWebClient;

	private final ClaudeApiProperties props;
	private final AiSystemPromptResolver promptResolver;

	@Value("${mock.enabled:true}")
	private boolean mockEnabled;

	@Value("${mock.claude-file:claude-summary.json}")
	private String mockFile;

	@CircuitBreaker(name = "CLAUDE_AI_CIRCUIT_BREAKER")
	@Retry(name = "CLAUDE_AI_RETRY")
	@RateLimiter(name = "CLAUDE_AI_RATE_LIMITER")
	@Override
	public CompletableFuture<String> summarize(AiType aiType, String content) {
		if (aiType != AiType.CLAUDE) {
			throw new IllegalArgumentException("이 어댑터는 CLAUDE 타입만 지원합니다.");
		}
		return summarize(content);
	}

	private CompletableFuture<String> summarize(String content) {
		if (mockEnabled) {
			return loadMockSummaryResponse();
		}

		// 시스템 프롬프트를 불러옴
		String systemPrompt = promptResolver.getPromptFor(AiType.CLAUDE);

		Map<String, Object> body = Map.of(
			"model", props.getModel(),
			"max_tokens", 500,
			"temperature", 0.3,
			"system", systemPrompt,
			"messages", List.of(
				Map.of("role", "user", "content", content)
			)
		);

		return claudeWebClient.post()
			.uri("/messages")
			.bodyValue(body)
			.retrieve()
			.onStatus(HttpStatusCode::isError, this::mapClaudeError)
			.bodyToMono(ClaudeResponse.class)
			.map(response -> {
				if (response == null || response.getContent().isEmpty()) {
					throw new IllegalStateException("Claude 요약 응답이 없습니다.");
				}
				return response.getContent().get(0).getText();
			})
			.toFuture();
	}

	private Mono<? extends Throwable> mapClaudeError(ClientResponse response) {
		return response.bodyToMono(ClaudeErrorResponse.class)
			.defaultIfEmpty(new ClaudeErrorResponse())
			.flatMap(err -> {
				ClaudeError error = err.getError();
				String vendorCode = null;
				String vendorMsg = null;

				if (error != null) {
					vendorMsg = error.getMessage();
					if (error.getErrors() != null && !error.getErrors().isEmpty()) {
						ClaudeErrorDetail detail = error.getErrors().getFirst();
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
			ClaudeResponse response = mapper.readValue(is, ClaudeResponse.class);
			String content = response.getContent().get(0).getText();
			return CompletableFuture.completedFuture(content);
		} catch (IOException e) {
			throw new RuntimeException("Claude mock 파일 로딩 실패", e);
		}
	}
}
