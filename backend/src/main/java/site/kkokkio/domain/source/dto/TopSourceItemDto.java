package site.kkokkio.domain.source.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.global.enums.Platform;

@Builder
@Schema(description = "실시간 인기 출처 목록의 개별 항목 DTO")
public record TopSourceItemDto(
        @NonNull String sourceId,
        @NonNull String url,
        @NonNull String title,
        String description,
        String thumbnailUrl,
        @NonNull LocalDateTime publishedAt,
        @NonNull Platform platform,
        int score
) {}
