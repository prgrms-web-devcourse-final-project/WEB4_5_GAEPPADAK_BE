package site.kkokkio.domain.keyword.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.controller.dto.KeywordMetricHourlyResponse;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.global.dto.RsData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/keywords")
public class KeywordMetricHourlyController {
	private final KeywordMetricHourlyService keywordMetricHourlyService;

	@GetMapping("/top")
	public RsData<List<KeywordMetricHourlyResponse>> getHourlyMetrics() {
		List<KeywordMetricHourlyResponse> responses = keywordMetricHourlyService.findHourlyMetrics();
		return new RsData<>(
			"200",
			"실시간 키워드를 불러왔습니다.",
			responses
		);
	}
}
