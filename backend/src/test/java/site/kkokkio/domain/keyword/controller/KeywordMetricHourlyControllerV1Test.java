package site.kkokkio.domain.keyword.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyResponse;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;

@WebMvcTest(controllers = KeywordMetricHourlyControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
public class KeywordMetricHourlyControllerV1Test {
	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private KeywordMetricHourlyService keywordMetricHourlyService;

	@Test
	@DisplayName("인기 10 키워드 조회 - 성공")
	public void getKeywordMetricHourly_Success() throws Exception {
		List<KeywordMetricHourlyResponse> mockKeywordList = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();
		for (long i = 1; i <= 10; i++) {
			mockKeywordList.add(
				new KeywordMetricHourlyResponse(i, "키워드 " + i, Platform.GOOGLE_TREND, now, 100, 100)
			);
		}

		given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(mockKeywordList);

		mvc.perform(get("/api/v1/keywords/top"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("실시간 키워드를 불러왔습니다."))
			.andExpect(jsonPath("$.data[0].text").value("키워드 1"))
			.andExpect(jsonPath("$.data[0].platform").value("GOOGLE_TREND"))
			.andExpect(jsonPath("$.data[0].volume").value(100))
			.andExpect(jsonPath("$.data[0].score").value(100));
	}

	@Test
	@DisplayName("인기 10 키워드 조회 - 잘못된 요청")
	public void getKeywordMetricHourly_Fail() throws Exception {
		given(keywordMetricHourlyService.findHourlyMetrics()).willThrow(new ServiceException("400", "키워드를 불러오지 못했습니다."));

		mvc.perform(get("/api/v1/keywords/top"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("키워드를 불러오지 못했습니다."));
	}
}
