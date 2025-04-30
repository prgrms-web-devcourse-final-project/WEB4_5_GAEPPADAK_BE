package site.kkokkio.domain.post.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Builder;

@Builder
public record PostListResponse(
	List<PostDto> list,
	PaginationMeta meta
) {
	@Builder
	private record PaginationMeta(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext,
		boolean hasPrevious
	) {
	}

	public static PostListResponse from(Page<PostDto> posts) {
		PaginationMeta paginationMeta = PaginationMeta.builder()
			.page(posts.getNumber())
			.size(posts.getSize())
			.totalElements(posts.getTotalElements())
			.totalPages(posts.getTotalPages())
			.hasNext(posts.hasNext())
			.hasPrevious(posts.hasPrevious())
			.build();

		return PostListResponse.builder()
			.list(posts.getContent())
			.meta(paginationMeta)
			.build();

	}
}
