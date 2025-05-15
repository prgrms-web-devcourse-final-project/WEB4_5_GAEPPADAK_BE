package site.kkokkio.infra.ai.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class GeminiApiProperties {
	@Value("${ai.gemini.key}")
	private String key;

	@Value("${ai.gemini.model}")
	private String model;

	@Value("${ai.gemini.base-url}")
	private String baseUrl;
}