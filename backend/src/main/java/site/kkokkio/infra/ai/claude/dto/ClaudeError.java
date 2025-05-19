package site.kkokkio.infra.ai.claude.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClaudeError {
	private String type;
	private String message;
	private List<ClaudeErrorDetail> errors;
}
