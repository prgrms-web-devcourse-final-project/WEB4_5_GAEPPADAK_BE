package site.kkokkio.infra.youtube.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// "errors" 배열 안의 각 상세 에러 항목을 담는 DTO
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeErrorDetail {

    // "reason" 필드 (구체적인 에러 원인 코드)
    private String reason;

    // "message" 필드 (상세 에러 메시지)
    private String message;
}

