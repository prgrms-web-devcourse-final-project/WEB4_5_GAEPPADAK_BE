package site.kkokkio.infra.youtube.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// "id" 객체를 담는 DTO
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceId {

    // "videoId" 필드
    @JsonProperty
    private String videoId;
}
