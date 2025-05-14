package site.kkokkio.infra.ai.gemini.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiError {
	private int code;
	private String message;
	private List<GeminiErrorDetail> errors;
}
