package site.kkokkio.global.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.mail.MessagingException;
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

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<RsData<Void>> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException e) {
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(
				new RsData<>(
					"400",
					"잘못된 요청입니다."
				));
	}

	// 서비스 로직에서 발생한 커스텀 예외(ServiceException) 처리
	@ResponseStatus // ResponseEntity.status()에 우선권이 있음
	@ExceptionHandler(ServiceException.class)
	public ResponseEntity<RsData<Void>> ServiceExceptionHandle(ServiceException ex) {

		return ResponseEntity
			.status(ex.getStatusCode()) // ex.getStatusCode() → RsData에서 파싱된 HTTP 상태
			.body(
				new RsData<>(
					ex.getCode(),
					ex.getMessage()
				)
			);
	}

	// 메일 전송(MessagingException) 에러 처리
	@ExceptionHandler(MessagingException.class)
	public ResponseEntity<RsData<Void>> handleMessagingException(MessagingException e) {
		e.printStackTrace();// 로깅
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(
				new RsData<>(
					"500",
					"이메일 인증 코드 전송 중 오류가 발생했습니다."
				)
			);
	}

	// Token 전역 예외 처리
	@ExceptionHandler(CustomAuthException.class)
	public ResponseEntity<RsData<Void>> handleCustomAuthException(CustomAuthException ex) {
		CustomAuthException.AuthErrorType type = ex.getAuthErrorType();

		RsData<Void> body = new RsData<>(
			type.getHttpStatus(),
			ex.getMessage()
		);

		String status;
		switch (type) {
			case MISSING_TOKEN:
			case TOKEN_EXPIRED:
			case CREDENTIALS_MISMATCH:
				status = "401";
				break;
			case MALFORMED_TOKEN:
			case UNSUPPORTED_TOKEN:
				status = "400";
				break;
			default:
				status = "500";
		}

		return ResponseEntity.status(Integer.parseInt(status)).body(body);
	}

}
