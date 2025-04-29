package site.kkokkio.infra.common.exception;

import site.kkokkio.global.exception.ServiceException;

/**
 * 잘못된 입력(4xx) → 즉시 실패
 */
public class ClientBadRequestException extends ServiceException {
    public ClientBadRequestException(int status, String msg){
        super(String.valueOf(status), msg);
    }
}