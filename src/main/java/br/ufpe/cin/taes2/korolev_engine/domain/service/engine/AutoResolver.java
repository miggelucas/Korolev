package br.ufpe.cin.taes2.korolev_engine.domain.service.engine;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoResolver {

    private final ValidationRunner validationRunner;

    /**
     * Attempts to fix errors in cascade. Performs up to 4 additional validation iterations.
     * Throws exception if it fails to converge.
     */
    public void resolve(Map<String, FeatureFlag> simulatedState, List<ValidationError> initialErrors) {
        int maxAdditionalIterations = 4;
        List<ValidationError> currentErrors = initialErrors;

        for (int i = 0; i < maxAdditionalIterations; i++) {
            log.info("[AutoResolver] - Auto-resolution iteration {} - Attempting to fix {} errors", i + 1, currentErrors.size());
            applyCorrections(currentErrors, simulatedState);
            
            currentErrors = validationRunner.executeValidation(simulatedState.values());
            if (currentErrors.isEmpty()) {
                return; // Successfully resolved
            }
        }

        // If loop finishes and there are still errors, it failed to converge
        log.error("[AutoResolver] - Auto-resolution failed - Could not converge to a valid state after {} total iterations", maxAdditionalIterations + 1);
        throw new FeatureFlagValidationException(
                "Falha ao tentar resolver conflitos automaticamente. O modelo pode estar em deadlock.", currentErrors);
    }

    private void applyCorrections(List<ValidationError> errors, Map<String, FeatureFlag> simulatedState) {
        for (ValidationError error : errors) {
            switch (error.getType()) {
                case HIERARCHY:
                    // Child active, parent inactive -> turn off child
                    simulatedState.get(error.getSourceFlag()).setActive(false);
                    break;
                case MANDATORY:
                    // Parent active, mandatory child inactive -> turn on child
                    simulatedState.get(error.getTargetFlag()).setActive(true);
                    break;
                case REQUIRES:
                    // Source active, target inactive -> turn off source
                    simulatedState.get(error.getSourceFlag()).setActive(false);
                    break;
                case EXCLUDES:
                    // Both active -> turn off target (arbitrary, could turn off source)
                    simulatedState.get(error.getTargetFlag()).setActive(false);
                    break;
                case SEMANTIC:
                    // Cannot be auto-resolved by toggling states
                    break;
            }
        }
    }
}
