package br.ufpe.cin.taes2.korolev_engine.application.usecase;

import br.ufpe.cin.taes2.korolev_engine.domain.service.UvlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application Use Case focused strictly on UVL import/export model operations.
 * Isolates model-level parsing/serialization concerns from resource-level CRUD operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UvlUseCase {

    private final UvlService uvlService;

    public void importUvl(String uvlContent) {
        log.info("[UvlUseCase] - Import UVL execution - Importing UVL model");
        uvlService.importUvl(uvlContent);
    }

    public String exportUvl() {
        log.info("[UvlUseCase] - Export UVL execution - Exporting UVL model");
        return uvlService.exportUvl();
    }
}
