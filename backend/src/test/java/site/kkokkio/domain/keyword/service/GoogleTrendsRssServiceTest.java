package site.kkokkio.domain.keyword.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.repository.KeywordRepository;

@SpringBootTest
@ActiveProfiles("test")
public class GoogleTrendsRssServiceTest {
	@Autowired
	private GoogleTrendsRssService googleTrendsRssService;

	@Autowired
	private KeywordRepository keywordRepository;

	@Test
	@DisplayName("키워드 추출 테스트")
	void getTrendingKeywordsFromRss_ParsesKeywords() {
		// when
		List<Keyword> trendingKeywords = googleTrendsRssService.getTrendingKeywordsFromRss();

		// then
		assertThat(trendingKeywords).isNotEmpty();
		for (Keyword keyword : trendingKeywords) {
			assertNotNull(keyword);
			assertThat(keyword.getText()).isNotBlank();
		}
		assertThat(trendingKeywords.size()).isEqualTo(10);

		// 데이터 베이스 저장 확인
		List<Keyword> savedKeywords = keywordRepository.findAll();
		assertThat(savedKeywords).isNotEmpty();
	}
}