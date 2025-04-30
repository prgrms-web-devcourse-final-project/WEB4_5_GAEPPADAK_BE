package site.kkokkio.domain.post.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Builder;
import site.kkokkio.global.dto.PaginationMeta;

@Builder
public record PostListResponse(
	List<PostDto> list,
	PaginationMeta meta
) {
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
