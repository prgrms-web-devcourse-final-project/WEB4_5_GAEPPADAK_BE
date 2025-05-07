package site.kkokkio.infra.naver.news;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverNewsSearchResponse {

    private List<NaverNewsSearchItem> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NaverNewsSearchItem {
        private String title;
        private String description;
        private String link;

        @JsonProperty("originallink")
        private String originalLink;

        @JsonFormat(pattern = "EEE, dd MMM yyyy HH:mm:ss Z", locale = "en")
        private OffsetDateTime pubDate;
    }
}
