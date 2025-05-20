package site.kkokkio.domain.keyword.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.global.init.BaseInitData;

@SpringBootTest
@ActiveProfiles("test")
public class TrendsServiceTest {
	@Autowired
	private TrendsService trendsService;

	@Autowired
	private KeywordRepository keywordRepository;

	@MockitoBean
	private BaseInitData baseInitData;

	@Test
	@DisplayName("키워드 추출 테스트")
	void getTrendingKeywordsFromRss_ParsesKeywords() {
		// when
		List<Keyword> trendingKeywords = trendsService.getTrendingKeywordsFromRss();

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