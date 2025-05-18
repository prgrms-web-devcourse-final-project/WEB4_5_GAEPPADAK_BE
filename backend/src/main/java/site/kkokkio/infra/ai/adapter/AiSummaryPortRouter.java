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
import site.kkokkio.infra.ai.claude.ClaudeAiApiPort;
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

	@Value("${ai.type.fallback}")
	private AiType fallbackAiType;

	public AiSummaryPortRouter(
		GeminiAiApiPort geminiAdapter,
		GptAiApiPort gptAdapter,
		ClaudeAiApiPort claudeAdapter
	) {
		this.delegateMap = new EnumMap<>(AiType.class);
		delegateMap.put(AiType.GEMINI, geminiAdapter);
		delegateMap.put(AiType.GPT, gptAdapter);
		delegateMap.put(AiType.CLAUDE, claudeAdapter);
	}

	@Override
	public CompletableFuture<String> summarize(AiType requestedAiType, String content) {
		// requestedAiType가 명시 되면 requestedAiType 우선. 아니라면 환경 변수에서 받아오기
		// Test 환경이나, 환경변수 외 따로 AI 선택 필요 시 사용
		AiType primaryAi = requestedAiType != null ? requestedAiType : currentAiType;
		AiType secondaryAi = (requestedAiType != null && requestedAiType.equals(backupAiType))
			? currentAiType
			: backupAiType;

		AiSummaryPort primaryAdapter = delegateMap.get(primaryAi);
		AiSummaryPort secondaryAdapter = delegateMap.get(secondaryAi);
		AiSummaryPort fallbackAdapter = delegateMap.get(fallbackAiType);

		if (primaryAdapter == null) {
			throw new IllegalArgumentException("지원하지 않는 AI 타입입니다: " + primaryAi);
		}

		// 메인 AI 오류 시 백업 AI가 폴백하여 요약
		return Mono.fromFuture(primaryAdapter.summarize(primaryAi, content))
			.onErrorResume(primaryError -> {
				if (secondaryAdapter != null && !primaryAi.equals(secondaryAi)) {
					log.warn("1차 AI '{}' 요약 실패, 2차 AI '{}'로 폴백합니다.", primaryAi, secondaryAi, primaryError);
					return Mono.fromFuture(secondaryAdapter.summarize(secondaryAi, content))
						.onErrorResume(secondaryError -> {
							if (fallbackAdapter != null && !secondaryAi.equals(fallbackAiType)) {
								log.warn("2차 AI '{}' 요약 실패, 3차 AI '{}'로 폴백합니다.", secondaryAi, fallbackAiType,
									secondaryError);
								return Mono.fromFuture(fallbackAdapter.summarize(fallbackAiType, content));
							} else {
								log.error("2차 AI 실패, 폴백할 AI가 없거나 동일합니다.", secondaryError);
								return Mono.error(secondaryError);
							}
						});
				} else {
					log.error("1차 AI 실패, 폴백할 AI가 없거나 동일합니다.", primaryError);
					return Mono.error(primaryError);
				}
			})
			.toFuture();
	}
}
