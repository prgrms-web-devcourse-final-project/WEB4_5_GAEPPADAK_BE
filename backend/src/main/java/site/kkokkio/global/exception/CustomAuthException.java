package site.kkokkio.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class CustomAuthException extends RuntimeException {

	private final AuthErrorType authErrorType;

	public CustomAuthException(AuthErrorType authErrorType, String message) {
		super(message);
		this.authErrorType = authErrorType;
	}

	@RequiredArgsConstructor
	public enum AuthErrorType {
		TOKEN_EXPIRED, // 토큰 만료
		UNSUPPORTED_TOKEN, // 지원되지 않는 토큰 형식
		MALFORMED_TOKEN, // 토큰 구조 오류
		CREDENTIALS_MISMATCH; // 인증 정보 불일치
	}
}
