package com.backset.forze.shared.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed.");
		problem.setTitle("Invalid request");
		problem.setType(URI.create("https://forze.local/problems/validation"));
		problem.setProperty("errors", validationErrors(exception));
		return problem;
	}

	@ExceptionHandler(ApiException.class)
	ProblemDetail handleAuth(ApiException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
		problem.setTitle(exception.status() == HttpStatus.CONFLICT ? "Conflict" : "Authentication failed");
		problem.setType(URI.create("https://forze.local/problems/authentication"));
		return problem;
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthentication(AuthenticationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication is required.");
		problem.setTitle("Authentication required");
		problem.setType(URI.create("https://forze.local/problems/authentication"));
		return problem;
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access is denied.");
		problem.setTitle("Forbidden");
		problem.setType(URI.create("https://forze.local/problems/authorization"));
		return problem;
	}

	private List<ValidationError> validationErrors(MethodArgumentNotValidException exception) {
		return exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(this::toValidationError)
				.toList();
	}

	private ValidationError toValidationError(FieldError error) {
		return new ValidationError(error.getField(), error.getDefaultMessage());
	}

	private record ValidationError(String field, String message) {
	}
}
