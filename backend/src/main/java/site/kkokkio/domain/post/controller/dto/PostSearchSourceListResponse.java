package site.kkokkio.domain.post.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.global.dto.PaginationMeta;
import site.kkokkio.global.enums.Platform;

@Builder
@Schema(description = "키워드 검색 출처 5개 최신순 조회 DTO (페이지네이션 포함)")
public record PostSearchSourceListResponse(
	List<SearchSourceList> list,
	PaginationMeta meta
) {

	public static PostSearchSourceListResponse from(List<SourceDto> sources, Page<PostDto> posts) {
		PaginationMeta paginationMeta = PaginationMeta.builder()
			.page(posts.getNumber())
			.size(posts.getSize())
			.totalElements(posts.getTotalElements())
			.totalPages(posts.getTotalPages())
			.hasNext(posts.hasNext())
			.hasPrevious(posts.hasPrevious())
			.build();

        return PostSearchSourceListResponse.builder()
                .list(sources.stream().map(SearchSourceList::from).toList())
				.meta(paginationMeta)
                .build();
    }

	@Builder
	private record SearchSourceList(
		@NonNull String sourceId,
		@NonNull String url,
		String thumbnailUrl,
		@NonNull String title,
		@NonNull Platform platform
		) {
		public static SearchSourceList from(SourceDto sourceDto) {
			return SearchSourceList.builder()
				.sourceId(sourceDto.sourceId())
				.url(sourceDto.url())
				.thumbnailUrl(sourceDto.thumbnailUrl())
				.title(sourceDto.title())
				.platform(sourceDto.platform())
				.build();
		}
	}
}
