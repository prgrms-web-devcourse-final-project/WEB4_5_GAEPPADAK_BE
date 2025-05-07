package site.kkokkio.global.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class UrlUtils {

	// 입력된 URL 정규화
	public static String normalize(String url) {
		try {
            UriComponents components = UriComponentsBuilder.fromUriString(url)
                .build()
                .normalize();

            return sortQueryParams(components);
        } catch (Exception e) {
            // 예외 발생 시 원본 URL 반환
            return url;
        }
	}

	// query parameter 재정렬
	private static String sortQueryParams(UriComponents components) {
		MultiValueMap<String, String> params = components.getQueryParams();

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
            .scheme(components.getScheme())
            .host(components.getHost())
			.path(components.getPath());

		if (!params.isEmpty()) {
			Map<String, List<String>> sorted = new TreeMap<>(params);
            sorted.forEach((key, values) -> values.forEach(v -> builder.queryParam(key, v)));
        }

        return builder.build().toUriString();
	}

	// HTML 태그 제거
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) return null;
        return Jsoup.parse(input).text();
    }
}