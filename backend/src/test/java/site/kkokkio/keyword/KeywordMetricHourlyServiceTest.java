package site.kkokkio.keyword;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyResponse;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class KeywordMetricHourlyServiceTest {
	@InjectMocks
	private KeywordMetricHourlyService keywordMetricHourlyService;

	@Mock
	private KeywordMetricHourlyRepository keywordMetricHourlyRepository;

	@Test
	@DisplayName("인기 키워드 조회 성공")
	void findHourlyMetricsTest() {
		// given
		List<KeywordMetricHourly> mockMetrics = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			Keyword keyword = Keyword.builder().id((long) i).text("키워드 " + i).build();
			KeywordMetricHourlyId id = KeywordMetricHourlyId.builder()
				.keywordId((long) i)
				.bucketAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(i))
				.platform(Platform.GOOGLE_TREND)
				.build();
			KeywordMetricHourly metric = KeywordMetricHourly.builder()
				.id(id)
				.keyword(keyword)
				.volume(i * 10)
				.score(i * 10)
				.build();
			mockMetrics.add(metric);
		}
		when(keywordMetricHourlyRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(mockMetrics);

		// when
		List<KeywordMetricHourlyResponse> responses = keywordMetricHourlyService.findHourlyMetrics();

		// then
		assertThat(responses).hasSize(10);
		for (int i = 0; i < 10; i++) {
			KeywordMetricHourlyResponse response = responses.get(i);
			assertThat(response.volume()).isEqualTo((i + 1) * 10);
			assertThat(response.score()).isEqualTo((i+ 1) * 10);
			assertThat(response.platform()).isEqualTo(Platform.GOOGLE_TREND);
			assertThat(response.keywordId()).isEqualTo((long) (i + 1));
			assertThat(response.bucketAt()).isNotNull();
		}
	}

	@Test
	@DisplayName("인기 키워드 조회 실패 - 키워드 존재 X")
	void findHourlyMetricsTest_Exception_WhenNoKeywordsFound() {
		// given
		when(keywordMetricHourlyRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(new ArrayList<>());

		// when
		// then
		ServiceException exception = assertThrows(ServiceException.class, () -> {
			keywordMetricHourlyService.findHourlyMetrics();
		});
		assertThat(exception.getCode()).isEqualTo("400");
		assertThat(exception.getMessage()).isEqualTo("키워드를 불러오지 못했습니다.");
	}

	@Test
	@DisplayName("인기 키워드 조회 실패 - null 반환")
	void findHourlyMetrics_Exception_WhenRepositoryReturnsNull() {
		// given
		when(keywordMetricHourlyRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(null);

		// when
		// then
		ServiceException exception = assertThrows(ServiceException.class, () -> {
			keywordMetricHourlyService.findHourlyMetrics();
		});
		assertThat(exception.getCode()).isEqualTo("400");
		assertThat(exception.getMessage()).isEqualTo("키워드를 불러오지 못했습니다.");
	}
}
