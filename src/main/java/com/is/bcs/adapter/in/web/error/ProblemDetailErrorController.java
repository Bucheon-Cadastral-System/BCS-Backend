package com.is.bcs.adapter.in.web.error;

import com.is.bcs.adapter.in.web.exception.CommonErrorCode;
import com.is.bcs.adapter.in.web.exception.ErrorDetailResolver;
import com.is.bcs.adapter.in.web.exception.SecurityErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.autoconfigure.error.AbstractErrorController;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * /error 로 떨어지는 응답을 ProblemDetail(RFC 9457) 포맷으로 통일한다 — BasicErrorController 대체.
 *
 * GlobalExceptionHandler(advice)는 DispatcherServlet 내부 예외만 잡는다. 서블릿 필터단 예외·컨테이너
 * sendError(413·정적 리소스 404 등)는 /error 재디스패치로 처리되는데, 기본 BasicErrorController는
 * ProblemDetail 포맷을 따르지 않아 여기서 정규화한다.
 * detail 문구는 ErrorDetailResolver — advice의 fallback 경로와 같은 문구를 쓴다.
 */
@RestController
public class ProblemDetailErrorController extends AbstractErrorController {

    private final Clock clock;
    private final ErrorDetailResolver errorDetailResolver;

    public ProblemDetailErrorController(ErrorAttributes errorAttributes, Clock clock, ErrorDetailResolver errorDetailResolver) {
        super(errorAttributes);
        this.clock = clock;
        this.errorDetailResolver = errorDetailResolver;
    }

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ProblemDetail> handle(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        if (!status.isError()) { // 에러 상태 없이 /error에 도달한 경우 500으로 정규화
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Map<String, Object> attrs = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, errorDetailResolver.detailFor(status));
        // 필터단 경로는 MVC의 instance 자동 채움이 없어 직접 설정
        problem.setInstance(URI.create(String.valueOf(attrs.getOrDefault("path", request.getRequestURI()))));
        problem.setProperty("code", codeFor(status));
        problem.setProperty("timestamp", OffsetDateTime.now(clock));

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private String codeFor(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED ->
                    SecurityErrorCode.AUTHENTICATION_REQUIRED.code();
            case FORBIDDEN ->
                    CommonErrorCode.AUTH_FORBIDDEN.code();
            default -> status.is5xxServerError()
                    ? CommonErrorCode.COMMON_INTERNAL_ERROR.code()
                    : CommonErrorCode.COMMON_BAD_REQUEST.code();
        };
    }

}
