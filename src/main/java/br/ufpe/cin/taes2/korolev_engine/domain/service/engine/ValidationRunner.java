package br.ufpe.cin.taes2.korolev_engine.domain.service.engine;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationRunner {

    private final List<ValidationRule<FeatureFlag>> rules;

    /**
     * Runs every registered {@link ValidationRule} against every flag in the collection.
     * Collects all validation errors across all rules and flags.
     * If any violations are found, aborts the operation with a descriptive exception containing all errors.
     */
    public void validateGlobalState(Collection<FeatureFlag> flags) {
        List<ValidationError> allErrors = executeValidation(flags);

        if (!allErrors.isEmpty()) {
            throw new FeatureFlagValidationException(
                    "Erros de validação encontrados no estado das features", allErrors);
        }
    }

    /**
     * Executes validation and returns the list of errors without throwing an exception.
     * Useful for loop-based auto-resolution.
     */
    public List<ValidationError> executeValidation(Collection<FeatureFlag> flags) {
        Map<String, FeatureFlag> flagMap = flags.stream()
                .collect(Collectors.toMap(FeatureFlag::getName, f -> f));

        log.debug("[ValidationRunner] - Global validation loop - Running global consistency validation on {} flags.", flags.size());

        List<ValidationError> allErrors = new ArrayList<>();

        for (FeatureFlag flag : flags) {
            for (ValidationRule<FeatureFlag> rule : rules) {
                log.debug("[ValidationRunner] - Validation rule execution - Running validation rule {} on flag {}", rule.getClass().getSimpleName(), flag.getName());
                List<ValidationError> ruleErrors = rule.validate(flag, flagMap);
                if (ruleErrors != null && !ruleErrors.isEmpty()) {
                    allErrors.addAll(ruleErrors);
                }
            }
        }
        return allErrors;
    }
}
