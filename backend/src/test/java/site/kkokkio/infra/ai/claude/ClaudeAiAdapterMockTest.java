package site.kkokkio.infra.ai.claude;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import site.kkokkio.infra.ai.AiType;

@SpringBootTest
@ActiveProfiles("test")
public class ClaudeAiAdapterMockTest {
	@Autowired
	private ClaudeAiApiPort claudeAiApiAdapter;

	@Test
	@DisplayName("Mock 모드에서 Claude 요약 요청 - 성공")
	void requestSummary_mockData() throws Exception {
		// given
		String content = "요약할 콘텐츠입니다.";

		// when
		CompletableFuture<String> resultFuture = claudeAiApiAdapter.summarize(AiType.CLAUDE, content);
		String result = resultFuture.get();

		// then
		assertThat(result).isNotNull();
		assertThat(result).contains("\"title\"");
		assertThat(result).contains("\"summary\"");
	}
}
