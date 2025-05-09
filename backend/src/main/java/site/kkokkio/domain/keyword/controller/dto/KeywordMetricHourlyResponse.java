package site.kkokkio.domain.keyword.controller.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyDto;
import site.kkokkio.global.enums.Platform;

@Builder
public record KeywordMetricHourlyResponse(
	@NonNull Long keywordId,
	@NonNull String text,
	@NonNull Platform platform,
	@NonNull LocalDateTime bucketAt,
	int score
	) {

	public static KeywordMetricHourlyResponse from(KeywordMetricHourlyDto dto) {
		return KeywordMetricHourlyResponse.builder()
			.keywordId(dto.keywordId())
			.text(dto.text())
			.platform(dto.platform())
			.bucketAt(dto.bucketAt())
			.score(dto.score()).build();
	}
}