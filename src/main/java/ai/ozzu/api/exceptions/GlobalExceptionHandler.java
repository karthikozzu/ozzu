package ai.ozzu.api.exceptions;

import ai.ozzu.api.generated.model.ApiError;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.exception.SQLGrammarException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingFieldException.class)
    public ResponseEntity<ApiError> handleMissingField(MissingFieldException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidJson(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request body or JSON format");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        if (message == null || message.isBlank()) {
            message = "Validation failed";
        }

        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));

        if (message == null || message.isBlank()) {
            message = "Validation failed";
        }

        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        String rootMessage = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        return error(HttpStatus.BAD_REQUEST, simplifyDatabaseMessage(rootMessage));
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiError> handleTransactionSystem(TransactionSystemException ex) {
        Throwable root = rootCause(ex);

        String message = root != null && root.getMessage() != null
                ? root.getMessage()
                : "Database transaction failed";

        return error(HttpStatus.BAD_REQUEST, simplifyDatabaseMessage(message));
    }

    @ExceptionHandler({
            JpaSystemException.class,
            org.hibernate.exception.ConstraintViolationException.class
    })
    public ResponseEntity<ApiError> handleJpaSystem(Exception ex) {
        Throwable root = rootCause(ex);

        String message = root != null && root.getMessage() != null
                ? root.getMessage()
                : "Database operation failed";

        return error(HttpStatus.BAD_REQUEST, simplifyDatabaseMessage(message));
    }

    @ExceptionHandler(SQLGrammarException.class)
    public ResponseEntity<ApiError> handleSqlGrammar(SQLGrammarException ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Database query failed");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }

    private String simplifyDatabaseMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Database operation failed";
        }

        String lower = rawMessage.toLowerCase();

        if (lower.contains("uk_wagers_user_event")
                || lower.contains("ux_wagers_user_event")) {
            return "User already has a wager for this event";
        }

        if (lower.contains("wagers_user_id_fkey")) {
            return "Invalid userId";
        }

        if (lower.contains("wagers_event_id_fkey")) {
            return "Invalid eventId";
        }

        if (lower.contains("wagers_domain_id_fkey")) {
            return "Invalid domainId";
        }

        if (lower.contains("wager_cards_wager_card_type_id_fkey")
                || lower.contains("fk_wager_cards_type")) {
            return "Invalid wagerCardTypeId";
        }

        if (lower.contains("wager_card_bindings_wager_card_type_binding_id_fkey")
                || lower.contains("fk_wcb_type_binding")) {
            return "Invalid wagerCardTypeBindingId";
        }

        if (lower.contains("wager_card_bindings_player_id_fkey")
                || lower.contains("fk_wcb_player")) {
            return "Invalid playerId";
        }

        if (lower.contains("wager_card_bindings_team_id_fkey")
                || lower.contains("fk_wcb_team")) {
            return "Invalid teamId";
        }

        if (lower.contains("wager_card_bindings_scoped_referent_id_fkey")
                || lower.contains("fk_wcb_scoped_ref")) {
            return "Invalid scopedReferentId";
        }

        if (lower.contains("fk_wcb_binding_value")
                || lower.contains("binding_value_id")) {
            return "Invalid bindingValueId";
        }

        if (lower.contains("duplicate key")) {
            return "Duplicate record is not allowed";
        }

        if (lower.contains("foreign key constraint")) {
            return "Invalid referenced id in request";
        }

        if (lower.contains("not-null constraint")) {
            return "Required field is missing";
        }

        if (lower.contains("check constraint")) {
            return "Invalid value failed database check constraint";
        }

        return "Database operation failed";
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;

        while (result != null && result.getCause() != null && result.getCause() != result) {
            result = result.getCause();
        }

        return result;
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        ApiError apiError = new ApiError();

        apiError.setCode(String.valueOf(status.value()));
        apiError.setMessage(
                message != null && !message.isBlank()
                        ? message
                        : status.getReasonPhrase()
        );

        return ResponseEntity.status(status).body(apiError);
    }
}