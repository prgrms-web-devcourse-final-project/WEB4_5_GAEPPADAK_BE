package site.kkokkio.infra.ai.gpt.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GptError {
	private int code;
	private String message;
	private List<GptErrorDetail> errors;
}
