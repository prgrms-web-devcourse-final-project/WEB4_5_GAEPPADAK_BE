package site.kkokkio.infra.common.exception;

import org.springframework.http.HttpStatusCode;

import lombok.NoArgsConstructor;
import site.kkokkio.global.exception.ServiceException;

@NoArgsConstructor
public final class ExternalApiErrorUtil {

    public static ServiceException of(HttpStatusCode status,
                                      String vendorCode,   // null 가능
                                      String vendorMsg) {

        int statusCode = status.value();

        String msg = vendorMsg != null && !vendorMsg.isBlank()
                   ? vendorMsg
                   : status.toString();

        if (vendorCode != null && !vendorCode.isBlank()) {
            msg = String.format("[%s] %s", vendorCode, msg);
        }
        return (statusCode == 429 || statusCode >= 500)
           ? new RetryableExternalApiException(statusCode, msg)
           : new ClientBadRequestException(statusCode, msg);
    }
}
