package site.kkokkio.infra.naver.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverErrorResponse {

    @JsonProperty("errorCode")
    private String code;

    @JsonProperty("errorMessage")
    private String message;
}