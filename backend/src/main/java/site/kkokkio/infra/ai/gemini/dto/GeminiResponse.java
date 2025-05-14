package site.kkokkio.infra.ai.gemini.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {

	// 전체 응답에서 choices 배열을 담고 있다.
	// choices는 여러개의 출력 후보를 담고 있다.
	// 보통 choices.get(0) 첫 번째 결과만 사용
	private List<Choice> choices;

	@Data
	public static class Choice {
		/** 생성된 메시지 */
		private Message message;
		/** 생성 종료 이유 (예: "stop") */
		@JsonProperty("finish_reason")
		private String finishReason;
		/** choice 인덱스 */
		private int index;
	}

	@Data
	public static class Message {
		/** 역할 (system / user) */
		private String role;
		/** 실제 텍스트 응답 */
		@JsonProperty("content")
		private String content;
	}

	// 토큰 사용량 관련. 일단 주석처리
	// @Data
	// public static class Usage {
	// 	@JsonProperty("prompt_tokens")
	// 	private int promptTokens;
	// 	@JsonProperty("completion_tokens")
	// 	private int completionTokens;
	// 	@JsonProperty("total_tokens")
	// 	private int totalTokens;
	// }

	// private Usage usage;
}
