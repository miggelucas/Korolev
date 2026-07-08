package br.ufpe.cin.taes2.korolev_engine.application.usecase;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.service.UvlService;
import br.ufpe.cin.taes2.korolev_engine.domain.service.engine.KorolevEngine;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application Use Case focused strictly on UVL import/export model operations.
 * Isolates model-level parsing/serialization concerns from resource-level CRUD operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UvlUseCase {

    private final UvlService uvlService;
    private final KorolevEngine korolevEngine;
    private final FeatureFlagRepository repository;

    public synchronized void importUvl(String uvlContent) {
        log.info("[UvlUseCase] - Import UVL execution - Importing UVL model");
        List<FeatureFlag> parsedFlags = uvlService.parseUvl(uvlContent);
        
        log.info("[UvlUseCase] - Import UVL - Validating integrity of {} parsed flags", parsedFlags.size());
        korolevEngine.validateGlobalState(parsedFlags);
        
        log.info("[UvlUseCase] - Import UVL - Validation passed. Committing imported flags to repository.");
        repository.clear();
        for (FeatureFlag flag : parsedFlags) {
            repository.save(flag);
        }
    }

    public String exportUvl() {
        log.info("[UvlUseCase] - Export UVL execution - Exporting UVL model");
        List<FeatureFlag> allFlags = repository.findAll();
        return uvlService.exportUvl(allFlags);
    }
}
