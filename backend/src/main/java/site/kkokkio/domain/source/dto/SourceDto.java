package site.kkokkio.domain.source.dto;

import lombok.Builder;
import org.springframework.lang.NonNull;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.global.enums.Platform;

import java.time.LocalDateTime;

@Builder
public record SourceDto(
        @NonNull String url,
        String thumbnailUrl,
        @NonNull String title,
        @NonNull LocalDateTime publishedAt,
        @NonNull Platform platform,
        String videoId
) {
    public static SourceDto from(Source source) {
        return SourceDto.builder()
                .url(source.getNormalizedUrl())
                .thumbnailUrl(source.getThumbnailUrl())
                .title(source.getTitle())
                .publishedAt(source.getPublishedAt())
                .platform(source.getPlatform())
                .videoId(source.getVideoId())
                .build();
    }
}