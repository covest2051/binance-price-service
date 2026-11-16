package com.example.binanceprice.controller;

import com.example.binanceprice.dto.response.ErrorResponse;
import com.example.binanceprice.dto.response.ErrorResponse.FieldViolation;
import com.example.binanceprice.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Clock;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String VALIDATION_FAILED = "Validation failed";

    private final Clock clock;

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Object> handleApplicationException(ApplicationException ex, WebRequest request) {
        return buildResponse(ex, ex.getStatus(), ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", List.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        return buildResponse(ex, status, VALIDATION_FAILED, bindingViolations(ex.getBindingResult()), request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldViolation> violations = ex.getParameterValidationResults().stream()
                .flatMap(GlobalExceptionHandler::parameterViolations)
                .toList();
        return buildResponse(ex, status, VALIDATION_FAILED, violations, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        FieldViolation violation = new FieldViolation(field, invalidValue(ex.getValue()));
        return buildResponse(ex, status, VALIDATION_FAILED, List.of(violation), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String message = body instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : ex.getMessage();
        return buildResponse(ex, statusCode, message, List.of(), request);
    }

    private ResponseEntity<Object> buildResponse(Exception ex, HttpStatusCode status, String message,
                                                 List<FieldViolation> violations, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        logException(ex, status, path, message, violations);
        ErrorResponse body = new ErrorResponse(clock.instant(), status.value(), reasonPhrase(status), message, path,
                violations);
        return ResponseEntity.status(status).body(body);
    }

    private static void logException(Exception ex, HttpStatusCode status, String path, String message,
                                     List<FieldViolation> violations) {
        if (status.is5xxServerError()) {
            log.error("Request {} failed with {}", path, status.value(), ex);
            return;
        }
        log.warn("Request {} rejected with {}: {} {}", path, status.value(), message, violations);
    }

    private static String reasonPhrase(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : status.toString();
    }

    private static List<FieldViolation> bindingViolations(Errors errors) {
        return errors.getAllErrors().stream()
                .map(error -> violation(errors.getObjectName(), error))
                .toList();
    }

    private static Stream<FieldViolation> parameterViolations(ParameterValidationResult result) {
        if (result instanceof ParameterErrors parameterErrors) {
            return bindingViolations(parameterErrors).stream();
        }
        String parameterName = result.getMethodParameter().getParameterName();
        return result.getResolvableErrors().stream().map(error -> violation(parameterName, error));
    }

    private static FieldViolation violation(String defaultField, MessageSourceResolvable error) {
        if (error instanceof FieldError fieldError) {
            String message = fieldError.isBindingFailure()
                    ? invalidValue(fieldError.getRejectedValue())
                    : fieldError.getDefaultMessage();
            return new FieldViolation(fieldError.getField(), message);
        }
        return new FieldViolation(defaultField, error.getDefaultMessage());
    }

    private static String invalidValue(Object value) {
        return "invalid value '%s'".formatted(value);
    }
}
