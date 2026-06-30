package br.ufpe.cin.taes2.korolev_engine.domain.validation;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import jakarta.xml.bind.ValidationException;

import java.util.Map;

/**
 * Strategy interface for feature flag validation rules.
 * Each implementation encapsulates a single validation concern
 * from the Software Product Line feature model.
 */
public interface ValidationRule {

    /**
     * Validates the given feature flag state against a specific constraint.
     * Throws FeatureFlagValidationException if the constraint is violated.
     *
     * @param flag    the feature flag being evaluated
     * @param flagMap a snapshot of all flags indexed by name for cross-reference lookups
     */
    void validate(FeatureFlag flag, Map<String, FeatureFlag> flagMap);
}
