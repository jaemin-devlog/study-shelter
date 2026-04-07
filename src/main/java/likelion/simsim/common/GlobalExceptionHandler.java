package likelion.simsim.common;

import jakarta.servlet.http.HttpServletRequest;
import likelion.simsim.common.exception.BadRequestException;
import likelion.simsim.common.exception.ConflictException;
import likelion.simsim.common.exception.NotFoundException;
import likelion.simsim.common.exception.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST 요청은 공통 JSON 포맷으로, SockJS 스트리밍 요청은 빈 상태 코드로 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        String message = fieldError == null
                ? "입력값이 올바르지 않습니다."
                : fieldError.getField() + ": " + fieldError.getDefaultMessage();

        return failResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException exception, HttpServletRequest request) {
        return failResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return failResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException exception, HttpServletRequest request) {
        return failResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException exception, HttpServletRequest request) {
        return failResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception exception, HttpServletRequest request) {
        return failResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", request);
    }

    private ResponseEntity<?> failResponse(HttpStatus status, String message, HttpServletRequest request) {
        if (isStreamingRequest(request)) {
            return ResponseEntity.status(status).build();
        }
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }

    private boolean isStreamingRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();

        return containsEventStream(contentType)
                || containsEventStream(accept)
                || (uri != null && uri.startsWith("/ws"));
    }

    private boolean containsEventStream(String value) {
        return value != null && value.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
