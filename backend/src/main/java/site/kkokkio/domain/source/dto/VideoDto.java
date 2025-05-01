package site.kkokkio.domain.source.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.util.HashUtils;

@Builder
public record VideoDto(
    String title,
    String link,
    String description,
    LocalDateTime pubDate
) {
    public Source toEntity(Platform platform) {
        return Source.builder()
                     .fingerprint(HashUtils.sha256Hex(link))
                     .normalizedUrl(link)
                     .title(title)
                     .description(description)
                     .thumbnailUrl(null)
                     .publishedAt(pubDate)
                     .platform(platform)
                     .build();
    }
}