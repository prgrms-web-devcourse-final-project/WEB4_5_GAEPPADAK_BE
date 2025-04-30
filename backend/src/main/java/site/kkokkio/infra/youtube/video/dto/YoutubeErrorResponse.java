package site.kkokkio.infra.youtube.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// YouTube API 에러 응답 전체를 담는 DTO
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeErrorResponse {

    // "error" 객체를 담는 필드
    private YoutubeError error;
}

