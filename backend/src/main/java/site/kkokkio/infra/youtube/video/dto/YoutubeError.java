package site.kkokkio.infra.youtube.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

// "error" 객체를 담는 DTO
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeError {

    // "code" 필드 (HTTP 상태 코드와 동일)
    private int code;

    // "message" 필드 (일반적인 에러 메시지)
    private String message;

    // "errors" 배열 (상세 에러 목록)
    private List<YoutubeErrorDetail> errors;
}
