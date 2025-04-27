package site.kkokkio.global.exception;

import site.kkokkio.global.dto.RsData;

public class ServiceException extends RuntimeException {

	private RsData<?> rsData;

	public ServiceException(String code, String message) {
		super(message);
		rsData = new RsData<>(code, message);
	}

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
