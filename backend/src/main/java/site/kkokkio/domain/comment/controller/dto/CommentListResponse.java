package site.kkokkio.domain.comment.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Builder;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.global.dto.PaginationMeta;

@Builder
public record CommentListResponse(
	List<CommentDto> list,
	PaginationMeta meta
) {
	public static CommentListResponse from(Page<CommentDto> comments) {
		PaginationMeta paginationMeta = PaginationMeta.of(
			comments.getNumber(),
			comments.getSize(),
			comments.getTotalElements(),
			comments.getTotalPages(),
			comments.hasNext(),
			comments.hasPrevious()
		);

		return CommentListResponse.builder()
			.list(comments.getContent())
			.meta(paginationMeta)
			.build();
	}
}
