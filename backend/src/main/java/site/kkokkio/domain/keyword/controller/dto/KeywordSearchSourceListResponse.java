package site.kkokkio.domain.keyword.controller.dto;

import java.util.List;

import org.springframework.lang.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.global.enums.Platform;

@Builder
@Schema(description = "키워드 검색 출처 5개 최신순 조회 DTO (페이지네이션 포함)")
public record KeywordSearchSourceListResponse(
	List<SearchSourceList> list
) {

	public static KeywordSearchSourceListResponse from(List<SourceDto> searchSourceList) {
        return KeywordSearchSourceListResponse.builder()
                .list(searchSourceList.stream().map(SearchSourceList::from).toList())
                .build();
    }

	@Builder
	private record SearchSourceList(
		@NonNull String url,
		String thumbnailUrl,
		@NonNull String title,
		@NonNull Platform platform
		) {
		public static SearchSourceList from(SourceDto sourceDto) {
			return SearchSourceList.builder()
				.url(sourceDto.url())
				.thumbnailUrl(sourceDto.thumbnailUrl())
				.title(sourceDto.title())
				.platform(sourceDto.platform())
				.build();
		}
	}
}
