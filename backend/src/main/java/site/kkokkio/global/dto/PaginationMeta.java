package site.kkokkio.global.dto;

import lombok.Builder;

@Builder
public record PaginationMeta(
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext,
	boolean hasPrevious
) {
	public static PaginationMeta of(
		int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious
	) {
		return PaginationMeta.builder()
			.page(page)
			.size(size)
			.totalElements(totalElements)
			.totalPages(totalPages)
			.hasNext(hasNext)
			.hasPrevious(hasPrevious)
			.build();
	}
}
