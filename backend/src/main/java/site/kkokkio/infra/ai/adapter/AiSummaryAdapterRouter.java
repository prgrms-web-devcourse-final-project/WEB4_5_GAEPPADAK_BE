package site.kkokkio.infra.ai.adapter;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import site.kkokkio.infra.ai.AiType;
import site.kkokkio.infra.ai.gemini.GeminiAiApiAdapter;

@Component
@Primary
public class AiSummaryAdapterRouter implements AiSummaryAdapter {

	private final Map<AiType, AiSummaryAdapter> delegateMap;

	public AiSummaryAdapterRouter(
		GeminiAiApiAdapter geminiAdapter
		// 추가 필요하면 추가
		// GptAiApiAdapter gptAdapter,
	) {
		this.delegateMap = new EnumMap<>(AiType.class);
		delegateMap.put(AiType.GEMINI, geminiAdapter);
		// 추가 필요하면 추가
		// delegateMap.put(AiType.GPT, gptAdapter);
		// delegateMap.put(AiType.CLAUDE, claudeAdapter);
	}

	@Override
	public CompletableFuture<String> summarize(AiType aiType, String content) {
		AiSummaryAdapter adapter = delegateMap.get(aiType);
		if (adapter == null) {
			throw new IllegalArgumentException("지원하지 않는 AI 타입입니다: " + aiType);
		}
		return adapter.summarize(aiType, content);
	}
}