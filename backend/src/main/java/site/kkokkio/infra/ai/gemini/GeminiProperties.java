package site.kkokkio.infra.ai.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class GeminiProperties {
	@Value("${ai.gemini.key}")
	private String key;

	// openai 호환 api 방식으로는 필요 없으나 문제 발생시 추가 예정
	// @Value("${ai.gemini.project-id}")
	// private String projectId;

	@Value("${ai.gemini.model}")
	private String model;

	@Value("${ai.gemini.base-url}")
	private String baseUrl;
}