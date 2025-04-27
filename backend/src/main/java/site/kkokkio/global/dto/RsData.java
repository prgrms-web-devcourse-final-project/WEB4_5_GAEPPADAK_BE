package site.kkokkio.global.dto;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RsData<T> {
	@NonNull
	private String code;
	@NonNull
	private String message;
	@NonNull
	private T data;

	// data 없이 code/message만 보내고 싶을 때ㅔ 빈 객체로 채우는 생성자
	public RsData(String code, String message) {
		this(code, message, (T)new Empty());
	}

	// getStatusCode()로 코드 앞부분을 잘라 HTTP 응답 상태로 사용
	@JsonIgnore
	public int getStatusCode() {
		String statusCodeStr = code.split("-")[0];
		return Integer.parseInt(statusCodeStr);
	}

}
