package site.kkokkio.infra.ai.gemini;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GeminiAiAdapterMockTest {

	@Autowired
	private GeminiClient geminiClient;

	@Test
	@DisplayName("Mock 모드에서 Gemini 요약 요청 - 성공")
	void requestSummary_mockData() throws Exception {
		// given: systemPrompt, content는 mock에서는 의미 없음
		CompletableFuture<String> result = geminiClient.requestSummaryAsync("시스템 프롬프트", "요약할 콘텐츠");

		// then: mock JSON에서 반환한 기대 값 확인
		assertThat(result).isNotNull();
		assertThat(result.get()).contains("\"title\"");
		assertThat(result.get()).contains("\"summary\"");
	}
}