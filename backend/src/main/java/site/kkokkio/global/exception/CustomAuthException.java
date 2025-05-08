package site.kkokkio.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class CustomAuthException extends RuntimeException {

	private final AuthErrorType authErrorType;

	public CustomAuthException(AuthErrorType authErrorType) {
		super(authErrorType.getDefaultMessage());
		this.authErrorType = authErrorType;
	}

	@RequiredArgsConstructor
	public enum AuthErrorType {
		MISSING_TOKEN("401", "인증 토큰이 없습니다."),
		TOKEN_EXPIRED("401", "토큰이 만료되어 인증할 수 없음"),
		UNSUPPORTED_TOKEN("400", "지원하지 않는 토큰 형식"),
		MALFORMED_TOKEN("400", "클라이언트가 보낸 토큰 자체가 형식적으로 잘못됨"),
		CREDENTIALS_MISMATCH("401", "토큰 서명 불일치 등으로 인증 실패");

		private final String httpStatus;
		private final String defaultMessage;

		public String getHttpStatus() {
			return httpStatus;
		}

		public String getDefaultMessage() {
			return defaultMessage;
		}
	}
}
