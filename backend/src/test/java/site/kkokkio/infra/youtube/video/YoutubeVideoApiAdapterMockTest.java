package site.kkokkio.infra.youtube.video;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import reactor.test.StepVerifier;
import site.kkokkio.domain.source.dto.VideoDto;


@SpringBootTest
@ActiveProfiles("test")
public class YoutubeVideoApiAdapterMockTest {

    @Autowired
    private YoutubeVideoApiAdapter youtubeVideoApiAdapter;

    @Test
    @DisplayName("Mock 모드에서 Youtube Mock JSON 파일로부터 Video 가져오기 - 성공")
    void fetchVideos_mockData() {
        // Mock 모드에서 fetchVideos 호출 (파라미터는 Mock 파일 로딩에 영향 없음)
        StepVerifier.create(youtubeVideoApiAdapter.fetchVideos("any_keyword", 10))
                // Mono<List<VideoDto>> 결과를 검증
                .assertNext(list -> {
                    // 리스트가 비어있지 않고 예상한 개수인지 검증
                    assertThat(list).isNotEmpty();
                    assertThat(list.size()).isEqualTo(3);

                    // 리스트의 첫번째 VideoDto 객체를 검증
                    VideoDto firstVideo = list.getFirst();

                    // mockVideoId1에 해당하는 데이터로 검증
                    assertThat(firstVideo.url()).isEqualTo("https://www.youtube.com/watch?v=mockVideoId1");
                    assertThat(firstVideo.title()).isEqualTo("Mock 영상 제목 1: 인기있는 컨텐츠");
                    assertThat(firstVideo.description()).isEqualTo(
                            "이 영상은 테스트 목적으로 생성된 가짜 영상입니다. 실제 데이터가 아닙니다.");
                    assertThat(firstVideo.thumbnailUrl()).isEqualTo(
                            "http://example.com/thumbnails/mockVideoId1/default.jpg");
                    assertThat(firstVideo.publishedAt()).isNotNull();
                }).verifyComplete();
    }
}
