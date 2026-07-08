package br.ufpe.cin.taes2.korolev_engine.application.usecase;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.service.GraphService;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphUseCase {

    private final GraphService graphService;
    private final FeatureFlagRepository repository;

    public String getGraphRepresentation() {
        log.debug("[GraphUseCase] - Get graph execution - Get ASCII graph representation");
        List<FeatureFlag> flags = repository.findAll();
        return graphService.renderGraph(flags);
    }
}
