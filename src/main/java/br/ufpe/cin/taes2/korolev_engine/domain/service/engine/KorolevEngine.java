package br.ufpe.cin.taes2.korolev_engine.domain.service.engine;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagAlreadyExistsException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagNotFoundException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core validation engine for the Korolev feature-flag management system.
 *
 * <p>This engine acts as a <b>Facade</b> for domain validation, orchestrating
 * the {@link ValidationRunner} and the {@link AutoResolver}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KorolevEngine {

    private final ValidationRunner validationRunner;
    private final AutoResolver autoResolver;

    @PostConstruct
    void logInitialization() {
        log.info("[KorolevEngine] - Engine initialization - Initialized KorolevEngine.");
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
            validationRunner.validateGlobalState(simulatedState.values());
            log.info("[KorolevEngine] - Creation validation success - Creation validation succeeded for feature flag: {}", flag.getName());
        } catch (RuntimeException ex) {
            log.warn("[KorolevEngine] - Creation validation failure - Creation validation failed for feature flag: {}. Reason: {}", flag.getName(), ex.getMessage());
            throw ex;
        }
    }

    /**
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

        // 3. Perform initial validation
        List<br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError> initialErrors = validationRunner.executeValidation(simulatedState.values());
        
        if (!initialErrors.isEmpty()) {
            if (!override) {
                log.warn("[KorolevEngine] - Bulk state update validation failure - Validation failed. Reason: {} errors", initialErrors.size());
                throw new FeatureFlagValidationException(
                        "Erros de validação encontrados no estado das features", initialErrors);
            }
            
            // Delegate to AutoResolver
            autoResolver.resolve(simulatedState, initialErrors);
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
            validationRunner.validateGlobalState(simulatedState.values());
            log.info("[KorolevEngine] - Deletion validation success - Deletion validation succeeded for feature flag: {}", name);
        } catch (RuntimeException ex) {
            log.warn("[KorolevEngine] - Deletion validation failure - Deletion validation failed for feature flag: {}. Reason: {}", name, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Runs global consistency validation on the given collection of flags.
     * Delegates to the ValidationRunner.
     */
    public void validateGlobalState(Collection<FeatureFlag> flags) {
        validationRunner.validateGlobalState(flags);
    }

    private Map<String, FeatureFlag> toMap(Collection<FeatureFlag> flags) {
        return flags.stream()
                .collect(Collectors.toMap(FeatureFlag::getName, f -> f, (a, b) -> b, HashMap::new));
    }
}
