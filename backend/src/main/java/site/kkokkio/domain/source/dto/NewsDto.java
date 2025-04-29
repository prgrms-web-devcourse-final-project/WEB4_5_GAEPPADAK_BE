package site.kkokkio.domain.source.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;
import site.kkokkio.domain.source.entity.Source;

@Value
@Builder
public class NewsDto {
    String title;
    String link;
    String originalLink;
    String description;
    LocalDateTime pubDate;

	public static NewsDto from(Source source) {
		return NewsDto.builder()
            .title(source.getTitle())
            .link(source.getNormalizedUrl())
            .originalLink(source.getNormalizedUrl())
            .description(source.getDescription())
            .pubDate(source.getPublishedAt())
			.build();
	}
}