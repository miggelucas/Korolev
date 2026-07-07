package br.ufpe.cin.taes2.korolev_engine.domain.exception;

import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError;
import lombok.Getter;
import java.util.List;

@Getter
public class FeatureFlagValidationException extends FeatureFlagException {
    
    private final List<ValidationError> errors;

    public FeatureFlagValidationException(String message) {
        super(message);
        this.errors = List.of(ValidationError.builder()
                .message(message)
                .build());
    }

    public FeatureFlagValidationException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = errors;
    }
}
