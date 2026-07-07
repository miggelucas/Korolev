package br.ufpe.cin.taes2.korolev_engine.domain.service;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagAlreadyExistsException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagNotFoundException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationRule;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core validation engine for the Korolev feature-flag management system.
 *
 * <p>This engine is a <b>pure validator</b>: it receives the current state as input,
 * applies validation rules, and either succeeds silently or throws a descriptive exception.
 * It has <b>no infrastructure dependencies</b> (no repository access).</p>
 *
 * <p>Uses the <b>Strategy pattern</b>: each feature-model constraint
 * (hierarchy, mandatory, cross-tree requires, mutual exclusion) is
 * encapsulated in its own {@link ValidationRule} implementation.</p>
 */
@Slf4j
@Service
public class KorolevEngine {

    private final List<ValidationRule<FeatureFlag>> rules;

    @Autowired
    public KorolevEngine(List<ValidationRule<FeatureFlag>> rules) {
        this.rules = rules;
    }

    @PostConstruct
    void logInitialization() {
        log.info("[KorolevEngine] - Engine initialization - Initialized KorolevEngine with {} validation rules.", rules.size());
    }

    /**
     * Validates the creation of a new feature flag against the current state.
     * Throws if the flag already exists or if the resulting state would be invalid.
     *
     * @param flag         the new flag to be created
     * @param currentState the current collection of all persisted flags
     */
    public void validateCreation(FeatureFlag flag, Collection<FeatureFlag> currentState) {
        if (flag == null || flag.getName() == null || flag.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Feature flag or name cannot be null or empty");
        }

        log.info("[KorolevEngine] - Creation validation start - Validating creation for feature flag: name={}, active={}", flag.getName(), flag.isActive());

        Map<String, FeatureFlag> simulatedState = toMap(currentState);

        if (simulatedState.containsKey(flag.getName())) {
            throw new FeatureFlagAlreadyExistsException("Feature flag already exists: " + flag.getName());
        }

        simulatedState.put(flag.getName(), flag);

        try {
            validateGlobalState(simulatedState.values());
            log.info("[KorolevEngine] - Creation validation success - Creation validation succeeded for feature flag: {}", flag.getName());
        } catch (RuntimeException ex) {
            log.warn("[KorolevEngine] - Creation validation failure - Creation validation failed for feature flag: {}. Reason: {}", flag.getName(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * Validates a bulk state update (active/inactive toggling) against the current state.
     * Applies the changes directly to the provided domain objects and returns the modified flags.
     * The caller is responsible for persisting the returned flags.
     *
     * <p><b>Contract:</b> The provided {@code currentState} objects may be mutated. The caller
     * should pass fresh copies (e.g. from a repository that returns domain objects via entity conversion).</p>
     * <p>
     * /**
     * Validates a bulk state update (active/inactive toggling) against the current state.
     * Optionally performs automatic dependency resolution (fallback) if override is true.
     *
     * @param states       a map of flag name → desired active state
     * @param currentState the current collection of all persisted flags
     * @param override     if true, attempts to automatically resolve validation errors
     * @return the list of flags with updated active states
     */
    public List<FeatureFlag> validateStateUpdate(Map<String, Boolean> states, Collection<FeatureFlag> currentState, boolean override) {
        if (states == null || states.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("[KorolevEngine] - Bulk state update validation start - Validating updates for {} flag states, override={}", states.size(), override);

        Map<String, FeatureFlag> simulatedState = toMap(currentState);
        
        // Save original states to detect what actually changed
        Map<String, Boolean> originalStates = new HashMap<>();
        for (FeatureFlag f : currentState) {
            originalStates.put(f.getName(), f.isActive());
        }

        // 1. Verify all target flags exist
        for (String name : states.keySet()) {
            if (!simulatedState.containsKey(name)) {
                throw new FeatureFlagNotFoundException("Feature flag not found for update: " + name);
            }
        }

        // 2. Apply initial state changes
        for (Map.Entry<String, Boolean> entry : states.entrySet()) {
            simulatedState.get(entry.getKey()).setActive(entry.getValue());
        }

        // 3. Validation Loop (up to 5 iterations for auto-resolution)
        int maxIterations = override ? 5 : 1;
        boolean valid = false;
        
        for (int i = 0; i < maxIterations; i++) {
            List<br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError> allErrors = executeValidation(simulatedState.values());
            
            if (allErrors.isEmpty()) {
                valid = true;
                break;
            }
            
            if (!override) {
                log.warn("[KorolevEngine] - Bulk state update validation failure - Validation failed. Reason: {} errors", allErrors.size());
                throw new br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException(
                        "Erros de validação encontrados no estado das features", allErrors);
            }
            
            log.info("[KorolevEngine] - Auto-resolution iteration {} - Attempting to fix {} errors", i + 1, allErrors.size());
            applyAutoResolution(allErrors, simulatedState);
        }
        
        if (!valid) {
            // Final check if auto-resolve failed to converge
            List<br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError> finalErrors = executeValidation(simulatedState.values());
            if (!finalErrors.isEmpty()) {
                log.error("[KorolevEngine] - Auto-resolution failed - Could not converge to a valid state after {} iterations", maxIterations);
                throw new br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException(
                        "Falha ao tentar resolver conflitos automaticamente. O modelo pode estar em deadlock.", finalErrors);
            }
        }

        log.info("[KorolevEngine] - Bulk state update validation success - Bulk state update validation succeeded");
        
        // Return only the ones that changed from the original collection to avoid persisting everything.
        List<FeatureFlag> changedFlags = new ArrayList<>();
        for (FeatureFlag f : simulatedState.values()) {
            if (originalStates.containsKey(f.getName()) && f.isActive() != originalStates.get(f.getName())) {
                changedFlags.add(f);
            }
        }
        
        return changedFlags;
    }

    private void applyAutoResolution(List<br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError> errors, Map<String, FeatureFlag> simulatedState) {
        for (br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError error : errors) {
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

    /**
     * Validates that deleting the specified flag would leave the remaining state consistent.
     *
     * @param name         the name of the flag to be deleted
     * @param currentState the current collection of all persisted flags
     */
    public void validateDeletion(String name, Collection<FeatureFlag> currentState) {
        if (name == null) {
            return;
        }

        Map<String, FeatureFlag> simulatedState = toMap(currentState);

        if (!simulatedState.containsKey(name)) {
            log.warn("[KorolevEngine] - Deletion validation check - Attempted to delete non-existent feature flag: {}", name);
            return;
        }

        log.info("[KorolevEngine] - Deletion validation start - Validating deletion for feature flag: name={}", name);

        simulatedState.remove(name);

        try {
            validateGlobalState(simulatedState.values());
            log.info("[KorolevEngine] - Deletion validation success - Deletion validation succeeded for feature flag: {}", name);
        } catch (RuntimeException ex) {
            log.warn("[KorolevEngine] - Deletion validation failure - Deletion validation failed for feature flag: {}. Reason: {}", name, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Runs every registered {@link ValidationRule} against every flag in the collection.
     * Collects all validation errors across all rules and flags.
     * If any violations are found, aborts the operation with a descriptive exception containing all errors.
     */
    public void validateGlobalState(Collection<FeatureFlag> flags) {
        List<br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError> allErrors = executeValidation(flags);

        if (!allErrors.isEmpty()) {
            throw new FeatureFlagValidationException(
                    "Erros de validação encontrados no estado das features", allErrors);
        }
    }

    private List<br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError> executeValidation(Collection<FeatureFlag> flags) {
        Map<String, FeatureFlag> flagMap = flags.stream()
                .collect(Collectors.toMap(FeatureFlag::getName, f -> f));

        log.debug("[KorolevEngine] - Global validation loop - Running global consistency validation on {} flags.", flags.size());

        List<br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError> allErrors = new ArrayList<>();

        for (FeatureFlag flag : flags) {
            for (ValidationRule<FeatureFlag> rule : rules) {
                log.debug("[KorolevEngine] - Validation rule execution - Running validation rule {} on flag {}", rule.getClass().getSimpleName(), flag.getName());
                List<br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError> ruleErrors = rule.validate(flag, flagMap);
                if (ruleErrors != null && !ruleErrors.isEmpty()) {
                    allErrors.addAll(ruleErrors);
                }
            }
        }
        return allErrors;
    }

    private Map<String, FeatureFlag> toMap(Collection<FeatureFlag> flags) {
        return flags.stream()
                .collect(Collectors.toMap(FeatureFlag::getName, f -> f, (a, b) -> b, HashMap::new));
    }
}
