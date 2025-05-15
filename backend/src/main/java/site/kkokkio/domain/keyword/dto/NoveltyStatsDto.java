package site.kkokkio.domain.keyword.dto;

import java.util.List;

public record NoveltyStatsDto(int lowVariationCount, List<Long> postableIds) {
}