package dev.toqash.travelplannerbackend.config;

import dev.toqash.travelplannerbackend.auth.EmailAlreadyUsedException;
import dev.toqash.travelplannerbackend.itinerary.ItineraryNotFoundException;
import dev.toqash.travelplannerbackend.planner.AiOutputInvalidException;
import dev.toqash.travelplannerbackend.planner.AiUnavailableException;
import dev.toqash.travelplannerbackend.planner.GenerationInProgressException;
import dev.toqash.travelplannerbackend.planner.GenerationJobNotFoundException;
import dev.toqash.travelplannerbackend.trip.TripNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    record FieldViolation(String field, String message) {
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ErrorCode.EMAIL_TAKEN);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        // Same response for unknown email and wrong password, so accounts can't be enumerated.
        return problem(HttpStatus.UNAUTHORIZED, "Invalid email or password", ErrorCode.INVALID_CREDENTIALS);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();

        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Request validation failed", ErrorCode.VALIDATION_FAILED);
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        // Full details go to the log only; the client gets a generic message.
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", ErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(TripNotFoundException.class)
    public ProblemDetail handleTripNotFound(TripNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ErrorCode.TRIP_NOT_FOUND);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleVersionConflict(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT,
                "The resource was changed by another request. Reload it and try again.",
                ErrorCode.VERSION_CONFLICT);
    }

    @ExceptionHandler(ItineraryNotFoundException.class)
    public ProblemDetail handleItineraryNotFound(ItineraryNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ErrorCode.ITINERARY_NOT_FOUND);
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleAiUnavailable(AiUnavailableException ex) {
        log.warn("AI provider unavailable: {}", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "30")
                .body(problem(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ErrorCode.AI_UNAVAILABLE));
    }

    @ExceptionHandler(AiOutputInvalidException.class)
    public ProblemDetail handleAiOutputInvalid(AiOutputInvalidException ex) {
        ProblemDetail body = problem(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), ErrorCode.AI_OUTPUT_INVALID);
        body.setProperty("errors", ex.getErrors());
        return body;
    }

    @ExceptionHandler(GenerationInProgressException.class)
    public ProblemDetail handleGenerationInProgress(GenerationInProgressException ex) {
        ProblemDetail body = problem(HttpStatus.CONFLICT, ex.getMessage(), ErrorCode.GENERATION_IN_PROGRESS);
        body.setProperty("activeJobId", ex.getActiveJobId());
        return body;
    }

    @ExceptionHandler(GenerationJobNotFoundException.class)
    public ProblemDetail handleJobNotFound(GenerationJobNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ErrorCode.JOB_NOT_FOUND);
    }

    private ProblemDetail problem(HttpStatus status, String detail, ErrorCode code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}