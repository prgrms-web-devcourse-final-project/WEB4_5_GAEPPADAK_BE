package site.kkokkio.infra.ai.gpt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class GptApiProperties {
	@Value("${ai.gpt.key}")
	private String key;

	@Value("${ai.gpt.model}")
	private String model;

	@Value("${ai.gpt.base-url}")
	private String baseUrl;
}
