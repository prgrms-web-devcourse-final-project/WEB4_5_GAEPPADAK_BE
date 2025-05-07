package site.kkokkio.global.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import site.kkokkio.global.dto.RsData;

@Aspect
@Component
@RequiredArgsConstructor
public class ResponseAspect {
	private final HttpServletResponse response;

	@Around("""
		(
		    within
		    (
		        @org.springframework.web.bind.annotation.RestController *
		    )
		    &&
		    (
		        @annotation(org.springframework.web.bind.annotation.GetMapping)
		        ||
		        @annotation(org.springframework.web.bind.annotation.PostMapping)
		        ||
		        @annotation(org.springframework.web.bind.annotation.PutMapping)
		        ||
		        @annotation(org.springframework.web.bind.annotation.DeleteMapping)
		    )
		)
		||
		@annotation(org.springframework.web.bind.annotation.ResponseBody)
		""")
	public Object responseAspect(ProceedingJoinPoint joinPoint) throws Throwable {

		// 실제 Controller 메서드(또는 @ResponseBody) 수행
		Object rst = joinPoint.proceed();

		// 돌려받은 객체가 RsData일 경우
		if (rst instanceof RsData rsData) {
			int statusCode = rsData.getStatusCode();// RsData.code의 앞부분을 꺼내
			response.setStatus(statusCode);// HttpServletResponse에 상태 코드로 설정
		}

		return rst; // 원래 리턴 객체를 그대로 반환 -> Json으로 변환 후 응답
	}
}
