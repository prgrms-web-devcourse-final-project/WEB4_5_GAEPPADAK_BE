package site.kkokkio.global.config; // 적절한 패키지 경로를 사용하세요

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration; // @Configuration 임포트
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.core.JsonParser; // JsonParser 임포트
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ObjectMapperConfig {

	@Bean
	// ObjectMapper 타입의 빈이 여러 개 있을 경우, 이 빈을 우선적으로 사용하도록 지정
	@Primary
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();

		// JSON 문자열 값 안에 이스케이프되지 않은 제어 문자(줄바꿈 등)를 허용
		// JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS 설정 추가
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

		return mapper;
	}
}