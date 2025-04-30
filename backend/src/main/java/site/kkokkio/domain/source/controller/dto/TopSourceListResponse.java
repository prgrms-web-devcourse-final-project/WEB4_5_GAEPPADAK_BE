package site.kkokkio.domain.source.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import site.kkokkio.domain.source.dto.TopSourceItemDto;
import site.kkokkio.global.dto.PaginationMeta;

@Builder
@Schema(description = "실시간 인기 출처 목록 응답 DTO (페이지네이션 포함)")
public record TopSourceListResponse(
        List<TopSourceItemDto> list,
        PaginationMeta meta
) {
	public static TopSourceListResponse from(Page<TopSourceItemDto> topSourceItemDtoPage) {
		PaginationMeta paginationMeta = PaginationMeta.of(
			topSourceItemDtoPage.getNumber(),
			topSourceItemDtoPage.getSize(),
			topSourceItemDtoPage.getTotalElements(),
			topSourceItemDtoPage.getTotalPages(),
			topSourceItemDtoPage.hasNext(),
			topSourceItemDtoPage.hasPrevious()
		);

        return TopSourceListResponse.builder()
                .list(topSourceItemDtoPage.getContent())
                .meta(paginationMeta)
                .build();
    }
}
