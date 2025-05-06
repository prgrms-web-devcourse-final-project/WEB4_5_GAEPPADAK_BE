package site.kkokkio.domain.source.dto;

import lombok.Builder;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.util.HashUtils;

import java.time.LocalDateTime;

/**
 * YouTube Data API 응답에서 변환되어 파이프라인 내에서 사용되는 비디오 정보 DTO
 * Source Entity로 변환 전 단계입니다.
 */
@Builder
public record VideoDto(
        String id,
        String title,
        String description,
        String thumbnailUrl,
        LocalDateTime publishedAt
) {

    /**
     * VideoDto를 Source Entity로 변환
     *
     * @param platform 플랫폼 정보 (Platform.YOUTUBE)
     * @return Source Entity
     */
    public Source toEntity(Platform platform) {

        // 유튜브 비디오 ID를 사용해 정규화된 URL 생성
        String normalizedUrl = "https://www.youtube.com/watch?v=" + this.id;

        // URL로 fingerprint 생성
        String fingerprint = HashUtils.sha256Hex(normalizedUrl);

        // 유튜브 썸네일은 여러 사이즈가 제공되는데, DTO에 담은 thumbnailUrl을 그대로 사용
        // 만약 API 응답에서 여러 URL을 받는다면, Adapter에서 변환 시 원하는
        // 사이즈의 URL을 선택하여 thumbnailUrl에 담아야 함
        return Source.builder()
                .fingerprint(fingerprint)
                .normalizedUrl(normalizedUrl)
                .title(this.title)
                .description(this.description)
                .thumbnailUrl(this.thumbnailUrl)
                .publishedAt(this.publishedAt)
                .platform(platform)
                .videoId(this.id)
                .build();
    }
}
