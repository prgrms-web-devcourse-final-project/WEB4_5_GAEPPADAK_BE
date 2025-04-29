package site.kkokkio.domain.keyword.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.service.GoogleTrendsRssService;

@RestController
@RequestMapping("/api/v1/keywords")
@RequiredArgsConstructor
@Tag(name = "Google Trend API", description = "구글 트렌드 API, 실사용 X")
public class GoogleTrendsController {

	private final GoogleTrendsRssService googleTrendsRssService;

	@GetMapping("trending-keywords")
	public List<String> getTrendingKeywordsApi() {
		return googleTrendsRssService.getTrendingKeywordsFromRss();
	}
}