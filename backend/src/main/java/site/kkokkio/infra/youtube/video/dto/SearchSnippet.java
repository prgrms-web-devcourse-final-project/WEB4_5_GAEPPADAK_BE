package site.kkokkio.infra.youtube.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

// "snippet" 객체를 담는 DTO
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchSnippet {

    // "publishedAt" 필드
    private OffsetDateTime publishedAt;
    private String title;
    private String description;
    private Map<String, ThumbnailDetails> thumbnails;
}
