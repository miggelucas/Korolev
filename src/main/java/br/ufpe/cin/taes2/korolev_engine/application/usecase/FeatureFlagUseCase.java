package br.ufpe.cin.taes2.korolev_engine.application.usecase;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.service.engine.KorolevEngine;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Application Use Case layer serving as the single orchestrator for all Feature Flag operations.
 * Centralizes repository access: loads current state, delegates pure validation to the engine,
 * and persists the results.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagUseCase {

    private final KorolevEngine korolevEngine;
    private final FeatureFlagRepository repository;

    public synchronized void createFlag(FeatureFlag flag) {
        log.info("[FeatureFlagUseCase] - Create flag execution - Creating flag named '{}'", flag.getName());

        List<FeatureFlag> currentState = repository.findAll();
        korolevEngine.validateCreation(flag, currentState);
        repository.save(flag);

        log.info("[FeatureFlagUseCase] - Create flag success - Flag '{}' persisted", flag.getName());
    }

    public synchronized void updateFlagStates(Map<String, Boolean> states, boolean override) {
        log.info("[FeatureFlagUseCase] - Update states execution - Updating active states for {} flags, override={}", states.size(), override);

        List<FeatureFlag> currentState = repository.findAll();
        List<FeatureFlag> updatedFlags = korolevEngine.validateStateUpdate(states, currentState, override);

        for (FeatureFlag flag : updatedFlags) {
            repository.save(flag);
        }

        log.info("[FeatureFlagUseCase] - Update states success - {} flag states persisted", updatedFlags.size());
    }

    public synchronized void deleteFlag(String name) {
        log.info("[FeatureFlagUseCase] - Delete flag execution - Deleting flag named '{}'", name);

        List<FeatureFlag> currentState = repository.findAll();
        korolevEngine.validateDeletion(name, currentState);
        repository.deleteByName(name);

        log.info("[FeatureFlagUseCase] - Delete flag success - Flag '{}' removed", name);
    }

    public List<FeatureFlag> getAllFlags() {
        log.debug("[FeatureFlagUseCase] - Get all flags execution - Fetch all flags");
        return repository.findAll();
    }

    public Optional<FeatureFlag> getFlagByName(String name) {
        log.debug("[FeatureFlagUseCase] - Get flag by name execution - Fetch flag by name '{}'", name);
        return repository.findByName(name);
    }
}
