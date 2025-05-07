package site.kkokkio.global.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import site.kkokkio.global.dto.Empty;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;
import site.kkokkio.global.exception.doc.ExampleHolder;

@Configuration
public class SwaggerConfig {

    @Value("${springdoc.swagger-ui.url}")
    private String swaggerServerUrl;

	// Swagger info 작성 (제목, 버전, 설명)
	@Bean
	public OpenAPI openAPI() {
		Info info = new Info()
			.title("꼬끼오 API")
			.version("Ver 1.0")
			.description("프로젝트 '꼬끼오' 서비스의 백엔드 API 명세서입니다.");

		return new OpenAPI()
			.info(info)
            .servers(List.of(
                new Server()
                    .url(swaggerServerUrl)
            ));
	}

	// API 응답을 커스터마이징하는 메서드
	// 특정 API에 @ApiErrorCodeExamples 어노테이션이 있는 경우, 예제 응답을 추가

	@Bean
	public OperationCustomizer customize() {
		return (Operation operation, HandlerMethod handlerMethod) -> {
			// 메서드에서 @ApiErrorCodeExamples 어노테이션을 가져옴
			ApiErrorCodeExamples apiErrorCodeExamples = handlerMethod.getMethodAnnotation(ApiErrorCodeExamples.class);
			if (apiErrorCodeExamples != null) {
				// API 응답에 에러 예제 추가
				addErrorExamplesToResponses(operation.getResponses(), apiErrorCodeExamples.value());
			}
			return operation;
		};
	}

	// API 응답에 에러 예제들을 추가하는 메서드
	private void addErrorExamplesToResponses(ApiResponses responses, ErrorCode[] errorCodes) {
		Map<Integer, List<ExampleHolder>> groupedExamples = Arrays.stream(errorCodes)
			.map(this::createExampleHolder)
			.collect(Collectors.groupingBy(ExampleHolder::code));

		groupedExamples.forEach((status, exampleHolders) -> {
			Content content = new Content();
			MediaType mediaType = new MediaType();
			ApiResponse apiResponse = new ApiResponse();

			// 각 ExampleHolder를 API 응답에 추가
			exampleHolders.forEach(holder -> mediaType.addExamples(holder.name(), holder.example()));

			// 응답 형식 설정, api 응답 추가
			content.addMediaType("application/json", mediaType);
			apiResponse.setContent(content);
			responses.addApiResponse(String.valueOf(status), apiResponse);
		});
	}

	// 에러 코드에 대한 예제 응답을 생성하는 메서드
	private ExampleHolder createExampleHolder(ErrorCode errorCode) {
		Example example = new Example();
		example.setValue(new RsData<Empty>(errorCode.getCode(), errorCode.getMessage()));

		return ExampleHolder.builder()
			.example(example)
			.code(Integer.parseInt(errorCode.getCode().split("-")[0]))
			.name(errorCode.name())
			.build();
	}
}