package site.kkokkio.domain.source.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;
import site.kkokkio.domain.source.dto.TopSourceItemDto;

import java.util.List;

@Builder
@Schema(description = "싱시간 인기 출처 목록 응답 DTO (페이지네이션 포함)")
public record TopSourceListResponse(
        List<TopSourceItemDto> list,
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

    public static TopSourceListResponse from(Page<TopSourceItemDto> topSourceItemDtoPage) {
        PaginationMeta paginationMeta = PaginationMeta.builder()
                .page(topSourceItemDtoPage.getNumber())
                .size(topSourceItemDtoPage.getSize())
                .totalElements(topSourceItemDtoPage.getTotalElements())
                .totalPages(topSourceItemDtoPage.getTotalPages())
                .hasNext(topSourceItemDtoPage.hasNext())
                .hasPrevious(topSourceItemDtoPage.hasPrevious())
                .build();

        return TopSourceListResponse.builder()
                .list(topSourceItemDtoPage.getContent())
                .meta(paginationMeta)
                .build();
    }
}
