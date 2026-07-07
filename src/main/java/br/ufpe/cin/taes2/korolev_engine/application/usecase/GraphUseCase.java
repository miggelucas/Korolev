package br.ufpe.cin.taes2.korolev_engine.application.usecase;

import br.ufpe.cin.taes2.korolev_engine.domain.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphUseCase {

    private final GraphService graphService;

    public String getGraphRepresentation() {
        log.debug("[GraphUseCase] - Get graph execution - Get ASCII graph representation");
        return graphService.renderGraph();
    }
}
