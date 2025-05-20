package site.kkokkio.infra.ai.claude.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClaudeResponse {
	@JsonProperty("content")
	private List<Content> content;

	@Data
	public static class Content {
		private String type;
		private String text;
	}
}
