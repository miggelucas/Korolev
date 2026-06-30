package br.ufpe.cin.taes2.korolev_engine.domain.service;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagAlreadyExistsException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagNotFoundException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
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
@RequiredArgsConstructor
public class KorolevEngine {

    private final List<ValidationRule> rules;

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
     *
     * @param states       a map of flag name → desired active state
     * @param currentState the current collection of all persisted flags (will be mutated)
     * @return the list of flags with updated active states
     */
    public List<FeatureFlag> validateStateUpdate(Map<String, Boolean> states, Collection<FeatureFlag> currentState) {
        if (states == null || states.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("[KorolevEngine] - Bulk state update validation start - Validating updates for {} flag states", states.size());

        Map<String, FeatureFlag> simulatedState = toMap(currentState);

        // 1. Verify all target flags exist
        for (String name : states.keySet()) {
            if (!simulatedState.containsKey(name)) {
                throw new FeatureFlagNotFoundException("Feature flag not found for update: " + name);
            }
        }

        // 2. Apply state changes directly (safe: repository returns fresh domain copies via entity conversion)
        List<FeatureFlag> updatedFlags = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : states.entrySet()) {
            FeatureFlag flag = simulatedState.get(entry.getKey());
            flag.setActive(entry.getValue());
            updatedFlags.add(flag);
        }

        // 3. Validate the complete resulting state
        try {
            validateGlobalState(simulatedState.values());
            log.info("[KorolevEngine] - Bulk state update validation success - Bulk state update validation succeeded");
        } catch (RuntimeException ex) {
            log.warn("[KorolevEngine] - Bulk state update validation failure - Bulk state update validation failed. Reason: {}", ex.getMessage());
            throw ex;
        }

        return updatedFlags;
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
     * The first rule violation found will abort the operation with a descriptive exception.
     */
    public void validateGlobalState(Collection<FeatureFlag> flags) {
        Map<String, FeatureFlag> flagMap = flags.stream()
                .collect(Collectors.toMap(FeatureFlag::getName, f -> f));

        log.debug("[KorolevEngine] - Global validation loop - Running global consistency validation on {} flags.", flags.size());
        for (FeatureFlag flag : flags) {
            for (ValidationRule rule : rules) {
                log.debug("[KorolevEngine] - Validation rule execution - Running validation rule {} on flag {}", rule.getClass().getSimpleName(), flag.getName());
                rule.validate(flag, flagMap);
            }
        }
    }

    private Map<String, FeatureFlag> toMap(Collection<FeatureFlag> flags) {
        return flags.stream()
                .collect(Collectors.toMap(FeatureFlag::getName, f -> f, (a, b) -> b, HashMap::new));
    }
}
