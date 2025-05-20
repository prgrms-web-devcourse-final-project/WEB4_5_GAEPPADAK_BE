package site.kkokkio.domain.post.port.out;

import java.util.concurrent.CompletableFuture;

import site.kkokkio.infra.ai.AiType;

public interface AiSummaryPort {
	/**
	 * 텍스트를 AI에게 요청하고 요약 문자열 반환
	 * 비동기로 전환
	 * **/
	CompletableFuture<String> summarize(AiType aiType, String content);
}
