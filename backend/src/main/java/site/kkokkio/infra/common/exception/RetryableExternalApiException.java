package site.kkokkio.infra.common.exception;

import site.kkokkio.global.exception.ServiceException;

/**
 * 429 또는 5xx → 재시도·서킷브레이커 대상
 */
public class RetryableExternalApiException extends ServiceException {
    public RetryableExternalApiException(int status, String msg){
        super(String.valueOf(status), msg);
    }
}