package site.kkokkio.domain.keyword.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyResponse;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.ServiceException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/keywords")
@Tag(name = "Keyword API", description = "인기 Keyword 관련 API")
public class KeywordMetricHourlyControllerV1 {
	private final KeywordMetricHourlyService keywordMetricHourlyService;

	@GetMapping("/top")
	@Operation(summary = "실시간 키워드 리스트", description = "실시간 키워드 Top 10개 보기")
	public RsData<List<KeywordMetricHourlyResponse>> getHourlyMetrics() {
		try {
			List<KeywordMetricHourlyResponse> responses = keywordMetricHourlyService.findHourlyMetrics();
			return new RsData<>(
				"200",
				"실시간 키워드를 불러왔습니다.",
				responses
			);
		} catch (ServiceException e) {
			return new RsData<>(
				e.getCode(),
				e.getMessage(),
				new ArrayList<>() // 또는 null
			);
		}
	}
}
