package site.kkokkio.infra.ai.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiErrorDetail {
	private String reason;
	private String message;
}
