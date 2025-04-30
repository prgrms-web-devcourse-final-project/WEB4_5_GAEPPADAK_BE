package site.kkokkio.domain.keyword.dto;

import java.time.LocalDateTime;

import lombok.NonNull;
import site.kkokkio.global.enums.Platform;

public record KeywordMetricHourlyDto(
	@NonNull Long keywordId,
	@NonNull String text,
	@NonNull Platform platform,
	@NonNull LocalDateTime bucketAt,
	int volume,
	int score
	) {
}
