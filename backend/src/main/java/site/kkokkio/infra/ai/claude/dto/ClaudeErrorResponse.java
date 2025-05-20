package site.kkokkio.infra.ai.claude.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClaudeErrorResponse {
	private ClaudeError error;
}
