package site.kkokkio.domain.source.controller.dto;

import java.util.List;

import org.springframework.lang.NonNull;

import lombok.Builder;
import site.kkokkio.domain.source.dto.SourceDto;

@Builder
public record SourceListResponse(
	List<SourceList> list
) {

	@Builder
	private record SourceList(
		@NonNull String sourceId,
		@NonNull String url,
		String thumbnailUrl,
		@NonNull String title
	) {
	}

	public static SourceListResponse from(List<SourceDto> sources) {
		return SourceListResponse.builder()
			.list(sources.stream()
				.map(source -> SourceList.builder()
					.sourceId(source.sourceId())
					.url(source.url())
					.thumbnailUrl(source.thumbnailUrl())
					.title(source.title())
					.build()
				).toList()
			)
			.build();
	}
}
