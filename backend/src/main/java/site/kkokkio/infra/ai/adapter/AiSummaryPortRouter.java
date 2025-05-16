package site.kkokkio.infra.ai.adapter;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import site.kkokkio.domain.post.port.out.AiSummaryPort;
import site.kkokkio.infra.ai.AiType;
import site.kkokkio.infra.ai.gemini.GeminiAiApiPort;
import site.kkokkio.infra.ai.gpt.GptAiApiPort;

@Component
@Primary
@Slf4j
public class AiSummaryPortRouter implements AiSummaryPort {

	private final Map<AiType, AiSummaryPort> delegateMap;

	@Value("${ai.type.current}")
	private AiType currentAiType;

	@Value("${ai.type.backup}")
	private AiType backupAiType;

	public AiSummaryPortRouter(
		GeminiAiApiPort geminiAdapter,
		GptAiApiPort gptAdapter
	) {
		this.delegateMap = new EnumMap<>(AiType.class);
		delegateMap.put(AiType.GEMINI, geminiAdapter);
		delegateMap.put(AiType.GPT, gptAdapter);
	}

	@Override
	public CompletableFuture<String> summarize(AiType requestedAiType, String content) {
		// requestedAiType가 명시 되면 requestedAiType 우선. 아니라면 환경 변수에서 받아오기
		// Test 환경을 위해서 작성
		AiType primaryAi = requestedAiType != null ? requestedAiType : currentAiType;
		AiType secondaryAi;

		// 만약 requestedAiType이 백업 Ai와 같다면 교환
		if (requestedAiType != null && requestedAiType.equals(backupAiType)) {
			secondaryAi = currentAiType;
		} else {
			secondaryAi = backupAiType;
		}

		AiSummaryPort primaryAdapter = delegateMap.get(primaryAi);
		AiSummaryPort secondaryAdapter = delegateMap.get(secondaryAi);

		if (primaryAdapter == null) {
			throw new IllegalArgumentException("지원하지 않는 AI 타입입니다: " + primaryAi);
		}

		return Mono.fromFuture(primaryAdapter.summarize(primaryAi, content))
			.onErrorResume(throwable -> {
				if (secondaryAdapter != null && !primaryAi.equals(secondaryAi) && delegateMap.containsKey(secondaryAi)) {
					log.warn("현재 AI '" + primaryAi + "' 요약 실패, 백업 AI '" + secondaryAi + "'로 폴백합니다.", throwable);
					return Mono.fromFuture(secondaryAdapter.summarize(secondaryAi, content));
				} else {
					log.error("현재 AI '" + primaryAi + "' 요약 실패, 폴백할 AI가 없거나 동일합니다.", throwable);
					return Mono.error(throwable);
				}
			})
			.toFuture();
	}
}