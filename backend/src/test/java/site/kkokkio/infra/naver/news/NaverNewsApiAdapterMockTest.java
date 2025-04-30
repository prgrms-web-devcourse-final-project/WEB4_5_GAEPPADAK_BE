package site.kkokkio.infra.naver.news;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import reactor.test.StepVerifier;
import site.kkokkio.domain.source.dto.NewsDto;

@SpringBootTest
@ActiveProfiles("test")
class NaverNewsApiAdapterMockTest {
    @Autowired
    private NaverNewsApiAdapter adapter;

    @Test
    @DisplayName("비운영환경에서 Mock JSON 파일로부터 News 가져오기 - 성공")
    void fetchNews_mockData() {
        StepVerifier.create(adapter.fetchNews("mock", 10, 1, "sim"))
                    .assertNext(list -> {
                        assertThat(list).isNotEmpty();
                        assertThat(list.size()).isEqualTo(10);
                        NewsDto first = list.getFirst();
                        assertThat(first.title()).isNotBlank();
                        assertThat(first.link()).startsWith("https://");
                        assertThat(first.pubDate()).isNotNull();
                    })
                    .verifyComplete();
    }
}