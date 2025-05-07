package site.kkokkio.domain.source.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.util.HashUtils;
import site.kkokkio.global.util.UrlUtils;

@Builder
public record NewsDto(
    String title,
    String link,
    String originalLink,
    String description,
    LocalDateTime pubDate
) {
    public Source toEntity(Platform platform) {
        String normalized = UrlUtils.normalize(link);
        String fingerprint = HashUtils.sha256Hex(normalized);
        String sanitizedTitle = UrlUtils.sanitize(title);
        String sanitizedDescription = UrlUtils.sanitize(description);
        return Source.builder()
                     .fingerprint(fingerprint)
                     .normalizedUrl(normalized)
                     .title(sanitizedTitle)
                     .description(sanitizedDescription)
                     .thumbnailUrl(null)
                     .publishedAt(pubDate)
                     .platform(platform)
                     .build();
    }
}