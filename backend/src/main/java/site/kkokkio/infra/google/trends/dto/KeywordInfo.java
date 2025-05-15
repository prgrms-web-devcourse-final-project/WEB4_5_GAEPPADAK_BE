package site.kkokkio.infra.google.trends.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KeywordInfo {
	private String text;
	private int volume;
}
