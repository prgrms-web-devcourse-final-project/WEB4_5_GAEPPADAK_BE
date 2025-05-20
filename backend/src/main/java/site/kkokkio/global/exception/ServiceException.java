package site.kkokkio.global.exception;

import site.kkokkio.global.dto.RsData;

public class ServiceException extends RuntimeException {

	private final RsData<?> rsData;
	
	public ServiceException(String code, String message) {
		super(message);
		rsData = new RsData<>(code, message);
	}

    // GlobalExceptionHandler에서 꺼내 쓸 수 있도록, RsData의 코드·메시지·상태코드 반환 메서드 제공
	public String getCode() {
		return rsData.getCode();
	}

	public String getMessage() {
		return rsData.getMessage();
	}

	public int getStatusCode() {
		return rsData.getStatusCode();
	}
}
