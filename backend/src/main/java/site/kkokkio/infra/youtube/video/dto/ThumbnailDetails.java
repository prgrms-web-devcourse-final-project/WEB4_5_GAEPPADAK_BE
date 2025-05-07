package site.kkokkio.infra.youtube.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// "thumbnails" 객체 안의 각 썸네일 상세 정보를 담는 DTO (기본, 중간, 고화질 등)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThumbnailDetails {
    private String url;
}
