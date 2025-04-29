package site.kkokkio.domain.source.dto;

import java.time.LocalDateTime;

import org.springframework.lang.NonNull;

import lombok.Builder;
import site.kkokkio.domain.source.entity.Source;

@Builder
public record SourceDto(
	@NonNull String url,
	@NonNull String thumbnailUrl,
	@NonNull String title,
	@NonNull LocalDateTime publishedAt
) {
	public static SourceDto from(Source source) {
		return SourceDto.builder()
			.url(source.getNormalizedUrl())
			.thumbnailUrl(source.getThumbnailUrl())
			.title(source.getTitle())
			.publishedAt(source.getPublishedAt())
			.build();
	}
}