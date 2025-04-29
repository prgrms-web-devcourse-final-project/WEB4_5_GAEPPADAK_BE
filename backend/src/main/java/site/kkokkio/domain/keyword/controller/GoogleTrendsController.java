package site.kkokkio.domain.keyword.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.service.GoogleTrendsRssService;

@RestController
@RequestMapping("/api/v1/keywords")
@RequiredArgsConstructor
public class GoogleTrendsController {

	private final GoogleTrendsRssService googleTrendsRssService;

	@GetMapping("trending-keywords")
	public List<String> getTrendingKeywordsApi() {
		return googleTrendsRssService.getTrendingKeywordsFromRss();
	}
}