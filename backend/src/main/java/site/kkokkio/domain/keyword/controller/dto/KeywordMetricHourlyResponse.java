package site.kkokkio.domain.keyword.controller.dto;

import java.time.LocalDateTime;

import lombok.NonNull;
import site.kkokkio.global.enums.Platform;

public record KeywordMetricHourlyResponse(
	@NonNull Long keywordId,
	@NonNull String text,
	@NonNull Platform platform,
	@NonNull LocalDateTime bucketAt,
	int volume,
	int score
	) {

}
