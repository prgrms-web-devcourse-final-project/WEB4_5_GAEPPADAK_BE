package site.kkokkio.domain.source.port.out;

import reactor.core.publisher.Mono;
import site.kkokkio.domain.source.dto.VideoDto;

import java.util.List;

/**
 * 외부 비디오 검색 API 호출용 포트 (Youtube Data API 활용)
 */
public interface VideoApiPort {

    /**
     * fetchVideos (비동기)
     * 주어진 키워드와 개수로 외부 비디오 검색 API를 호출하여 결과를 가져옵니다.
     *
     * @param keyword 검색할 키워드
     * @param count 가져올 비디오 개수
     * @return 비디오 정보를 담은 YoutubeVideoDto 목록을 발행하는 Mono
     */
    Mono<List<VideoDto>> fetchVideos(String keyword, int count);
}
