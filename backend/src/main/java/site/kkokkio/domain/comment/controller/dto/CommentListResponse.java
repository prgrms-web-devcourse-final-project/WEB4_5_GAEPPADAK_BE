package site.kkokkio.domain.comment.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Builder;
import site.kkokkio.domain.comment.dto.CommentDto;

@Builder
public record CommentListResponse(
	List<CommentDto> list,
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

	public static CommentListResponse from(Page<CommentDto> comments) {
		PaginationMeta paginationMeta = PaginationMeta.builder()
			.page(comments.getNumber())
			.size(comments.getSize())
			.totalElements(comments.getTotalElements())
			.totalPages(comments.getTotalPages())
			.hasNext(comments.hasNext())
			.hasPrevious(comments.hasPrevious())
			.build();

		return CommentListResponse.builder()
			.list(comments.getContent())
			.meta(paginationMeta)
			.build();
	}
}