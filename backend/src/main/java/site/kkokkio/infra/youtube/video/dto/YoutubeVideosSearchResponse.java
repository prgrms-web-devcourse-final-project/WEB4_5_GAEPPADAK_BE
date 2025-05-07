package site.kkokkio.infra.youtube.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

// Youtube API 응답 전체를 담는 DTO
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeVideosSearchResponse {

    // "items" 배열을 List로 매핑
    private List<YoutubeSearchItem> items;
}
