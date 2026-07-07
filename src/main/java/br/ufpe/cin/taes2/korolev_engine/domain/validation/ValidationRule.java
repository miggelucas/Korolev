package br.ufpe.cin.taes2.korolev_engine.domain.validation;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import java.util.List;
import java.util.Map;

/**
 * Strategy interface for feature flag validation rules.
 * Each implementation encapsulates a single validation concern
 * from the Software Product Line feature model.
 */
public interface ValidationRule<T> {

    /**
     * Validates the given entity state against a specific constraint.
     * Returns a list of validation errors if constraints are violated.
     *
     * @param entity  the entity being evaluated
     * @param contextMap a snapshot of all entities indexed by name for cross-reference lookups
     * @return a list of errors, or an empty list if valid
     */
    List<ValidationError> validate(T entity, Map<String, T> contextMap);

    /**
     * Defines the specific error type that this rule enforces.
     */
    ErrorType getErrorType();

    /**
     * Helper method to construct a ValidationError bound to this rule's ErrorType.
     */
    default ValidationError buildError(String source, String target, String message) {
        return ValidationError.builder()
                .type(getErrorType())
                .sourceFlag(source)
                .targetFlag(target)
                .message(message)
                .build();
    }
}
