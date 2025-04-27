package site.kkokkio.global.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import site.kkokkio.global.dto.RsData;

@RestControllerAdvice
public class GlobalExceptionHandler {

	// @Valid 검증 실패 시
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<RsData<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

		// 검증 오류 메시지들 한줄로 합치기
		String message = e.getBindingResult().getFieldErrors()
			.stream()
			.map(fe -> fe.getField() + " : " + fe.getCode() + " : " + fe.getDefaultMessage())
			.sorted()
			.collect(Collectors.joining("\n"));

		// 400 코드로 RsData 생성 후 BAD_REQUEST(400) 상태로 응답
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(
				new RsData<>(
					"400",
					message
				)
			);
	}
}
