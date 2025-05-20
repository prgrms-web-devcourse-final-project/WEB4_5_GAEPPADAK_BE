package site.kkokkio.infra.ai.gpt.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GptResponse {
	private List<Choice> choices;

	@Data
	public static class Choice {
		private Message message;
		@JsonProperty("finish_reason")
		private String finishReason;
		private int index;
	}

	@Data
	public static class Message {
		private String role;
		private String content;
	}
}
