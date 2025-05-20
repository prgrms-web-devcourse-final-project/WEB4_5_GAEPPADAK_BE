package site.kkokkio.infra.ai.claude;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class ClaudeApiProperties {
	@Value("${ai.claude.key}")
	private String key;

	@Value("${ai.claude.model}")
	private String model;

	@Value("${ai.claude.base-url}")
	private String baseUrl;
}
