package site.kkokkio.infra.ai.gpt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.core.publisher.Mono;
import site.kkokkio.infra.ai.AiType;
import site.kkokkio.infra.ai.adapter.AiSummaryPortRouter;
import site.kkokkio.infra.common.exception.RetryableExternalApiException;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"mock.enabled=false",
	"ai.type.current=GPT",
	"ai.type.backup=GPT", // 폴백 방지
	"ai.type.tertiary=GPT" // 폴백 방지
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GptAiAdpaterTest {
	@TestConfiguration
	static class Config {
		@Bean
		ExchangeFunction ef() {
			return Mockito.mock(ExchangeFunction.class);
		}

		@Bean
		@Qualifier("gptWebClient")
		WebClient gptWebClient(ExchangeFunction ef) {
			return WebClient.builder()
				.exchangeFunction(ef)
				.build();
		}

		@Bean
		GptApiProperties props() {
			var p = new GptApiProperties();
			p.setBaseUrl("https://fake");
			p.setKey("fake-key");
			return p;
		}
	}

	@Autowired
	ExchangeFunction ef;

	@Autowired
	AiSummaryPortRouter aiSummaryPortRouter;

	@Autowired
	CircuitBreakerRegistry cbRegistry;

	private ClientResponse resp(String body, HttpStatus s) {
		return ClientResponse.create(s, ExchangeStrategies.withDefaults())
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(body).build();
	}

	@Test
	@DisplayName("Gpt 요약 호출 - 성공 (Async)")
	void summary_success_async() throws Exception {
		String gptResponseJson = """
			{
			  "choices": [
			    {
			      "message": {
			        "content": "{\\"title\\":\\"T\\",\\"summary\\":\\"S\\"}"
			      }
			    }
			  ]
			}
			""";

		when(ef.exchange(any()))
			.thenReturn(Mono.just(resp(gptResponseJson, HttpStatus.OK)));
		CompletableFuture<String> result = aiSummaryPortRouter.summarize(AiType.GPT, "user");
		assertThat(result).isNotNull();
		assertThat(result.get()).isEqualTo("{\"title\":\"T\",\"summary\":\"S\"}");
	}

	@Test
	@DisplayName("Gpt 요약 호출 - 실패 후 circuit breaker 작동")
	void summary_retry_and_cb_async() throws Exception {
		String errBody = """
			{
			  "error": {
			    "code": 503,
			    "message": "Service unavailable",
			    "errors": [
			      {
			        "reason": "SERVICE_UNAVAILABLE",
			        "message": "Temporary outage"
			      }
			    ]
			  }
			}
			""";

		// 첫 번째 호출: 503 에러 응답 시뮬레이션
		when(ef.exchange(any()))
			.thenReturn(Mono.just(resp(errBody, HttpStatus.SERVICE_UNAVAILABLE)));

		// 첫 번째 요청 → RetryableExternalApiException 발생 기대
		CompletableFuture<String> future = aiSummaryPortRouter.summarize(AiType.GPT, "y");

		assertThatThrownBy(future::get)
			.hasCauseInstanceOf(RetryableExternalApiException.class);

		// circuit breaker registry에서 같은 이름으로 직접 얻어올 수 있도록 보장
		CircuitBreaker cb = cbRegistry.circuitBreaker("GPT_AI_CIRCUIT_BREAKER");
		assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

		// 두 번째 호출: Circuit breaker 열려 있어서 바로 실패
		CompletableFuture<String> blocked = aiSummaryPortRouter.summarize(AiType.GPT, "y");
		assertThatThrownBy(blocked::get)
			.hasCauseInstanceOf(CallNotPermittedException.class);

		verify(ef, times(2)).exchange(any());
	}

	@Test
	@DisplayName("Gpt 요약 호출 - rateLimiter 확인")
	void summary_rateLimiter_blocked() throws Exception {
		// given
		String successBody = """
			{
			  "choices": [
			    {
			      "message": {
			        "content": "{\\"title\\":\\"t\\",\\"summary\\":\\"s\\"}"
			      }
			    }
			  ]
			}
			""";

		when(ef.exchange(any()))
			.thenReturn(
				Mono.just(resp(successBody, HttpStatus.OK)),
				Mono.just(resp(successBody, HttpStatus.OK)),
				Mono.just(resp(successBody, HttpStatus.OK))
			);

		// when
		CompletableFuture<String> f1 = aiSummaryPortRouter.summarize(AiType.GPT, "u");
		CompletableFuture<String> f2 = aiSummaryPortRouter.summarize(AiType.GPT, "u");

		// then
		assertThat(f1.get()).contains("title");
		assertThat(f2.get()).contains("summary");

		// 3번째 요청은 RateLimiter에 막힘 예상
		assertThatThrownBy(() -> aiSummaryPortRouter.summarize(AiType.GPT, "u").get())
			.hasCauseInstanceOf(io.github.resilience4j.ratelimiter.RequestNotPermitted.class);

		verify(ef, times(2)).exchange(any());
	}
}
