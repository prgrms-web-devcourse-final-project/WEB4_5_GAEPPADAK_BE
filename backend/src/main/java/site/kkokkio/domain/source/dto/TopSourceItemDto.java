package site.kkokkio.domain.source.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.global.enums.Platform;

import java.time.LocalDateTime;

@Builder
@Schema(description = "실시간 인기 출처 목록의 개별 항목 DTO")
public record TopSourceItemDto(
        @NonNull String url,
        @NonNull String title,
        @NonNull String thumbnailUrl,
        @NonNull LocalDateTime publishedAt,
        @NonNull Platform platform,
        int score
) {
    public static TopSourceItemDto fromSource(Source source) {
        return TopSourceItemDto.builder()
                .url(source.getNormalizedUrl())
                .title(source.getTitle())
                .thumbnailUrl(source.getThumbnailUrl())
                .publishedAt(source.getPublishedAt())
                .platform(source.getPlatform())
                .score(0)
                .build();
    }
}
