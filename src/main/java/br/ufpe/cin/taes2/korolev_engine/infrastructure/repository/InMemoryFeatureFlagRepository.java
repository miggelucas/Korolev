package br.ufpe.cin.taes2.korolev_engine.infrastructure.repository;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.entity.FeatureFlagEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class InMemoryFeatureFlagRepository implements FeatureFlagRepository {

    private final Map<String, FeatureFlagEntity> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<FeatureFlag> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        FeatureFlagEntity entity = storage.get(name);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<FeatureFlag> findAll() {
        return storage.values().stream()
                .map(FeatureFlagEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public FeatureFlag save(FeatureFlag flag) {
        if (flag == null || flag.getName() == null) {
            throw new IllegalArgumentException("Feature flag name cannot be null");
        }
        log.info("[InMemoryFeatureFlagRepository] - Write save - Saving feature flag to in-memory store: name={}, active={}", flag.getName(), flag.isActive());
        FeatureFlagEntity entity = FeatureFlagEntity.fromDomain(flag);
        storage.put(flag.getName(), entity);
        return entity.toDomain();
    }

    @Override
    public void deleteByName(String name) {
        if (name != null) {
            log.info("[InMemoryFeatureFlagRepository] - Write delete - Deleting feature flag from in-memory store: name={}", name);
            storage.remove(name);
        }
    }

    @Override
    public void clear() {
        log.debug("[InMemoryFeatureFlagRepository] - Database cleanup - Clearing all feature flags from repository");
        storage.clear();
    }
}
