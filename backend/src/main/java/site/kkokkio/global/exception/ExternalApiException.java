package site.kkokkio.global.exception;

public class ExternalApiException extends ServiceException {

	public ExternalApiException(String message) {
		super("503", message);
	}

	public ExternalApiException(String message, Throwable cause) {
		super("503", message);
		this.initCause(cause);
	}
}