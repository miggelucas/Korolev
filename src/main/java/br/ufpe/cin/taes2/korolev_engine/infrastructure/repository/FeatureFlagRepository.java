package br.ufpe.cin.taes2.korolev_engine.infrastructure.repository;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagRepository {
    Optional<FeatureFlag> findByName(String name);
    List<FeatureFlag> findAll();
    FeatureFlag save(FeatureFlag flag);
    void deleteByName(String name);
    void clear();
}
