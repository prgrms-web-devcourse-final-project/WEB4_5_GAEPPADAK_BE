package site.kkokkio.infra.ai.gpt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GptErrorResponse {
	GptError error;
}
