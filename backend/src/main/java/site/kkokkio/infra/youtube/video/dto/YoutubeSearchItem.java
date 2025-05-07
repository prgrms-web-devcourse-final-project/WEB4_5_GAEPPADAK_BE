package site.kkokkio.infra.youtube.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// "items" 배열 안의 각 검색 결과 항목을 담는 DTO
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeSearchItem {
    private ResourceId id;
    private SearchSnippet snippet;
}
