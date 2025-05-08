package site.kkokkio.infra.ai.adapter;

public interface AiSummaryClient {
	/**
	 * 텍스트를 AI에게 요청하고 요약 문자열 반환
	 * **/
	String requestSummary(String systemPrompt, String content);
}
