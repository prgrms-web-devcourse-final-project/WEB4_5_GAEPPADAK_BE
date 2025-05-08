package site.kkokkio.infra.ai.gemini;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import site.kkokkio.infra.ai.adapter.AiSummaryClient;
import site.kkokkio.infra.ai.gemini.dto.GeminiResponse;

@Component
public class GeminiClient implements AiSummaryClient {

	private final WebClient webClient;
	private final GeminiProperties props;

	public GeminiClient(
		@Qualifier("geminiWebClient")
		WebClient geminiWebClient,
		GeminiProperties props
	) {
		this.webClient = geminiWebClient;
		this.props = props;
	}

	@Override
	public String requestSummary(String systemPrompt, String content) {
		Map<String, Object> body = Map.of(
			"model", props.getModel(),
			"messages", List.of(
				Map.of("role", "system", "content", systemPrompt),
				Map.of("role", "user", "content", content)
			)
		);
		GeminiResponse response = webClient.post()
			.uri("/chat/completions")
			.bodyValue(body)
			.retrieve()
			.bodyToMono(GeminiResponse.class)
			.block();

		if (response == null || response.getChoices().isEmpty()) {
			throw new IllegalStateException("Gemini 요약 응답이 없습니다.");
		}
		return response.getChoices().get(0).getMessage().getContent();
	}
}
