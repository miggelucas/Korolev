package br.ufpe.cin.taes2.korolev_engine.controller;

import br.ufpe.cin.taes2.korolev_engine.application.usecase.GraphUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/flags")
@RequiredArgsConstructor
@Tag(name = "Graph Representation", description = "Operations for generating feature flag dependency graphs")
public class GraphController {

    private final GraphUseCase graphUseCase;

    @GetMapping(value = "/graph", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "Render the feature flag dependency graph",
            description = "Returns an ASCII tree-like representation of the feature flags and their hierarchy/dependencies."
    )
    public ResponseEntity<String> getGraph() {
        log.info("[GraphController] - Handle render graph request - GET /api/flags/graph");
        String graph = graphUseCase.getGraphRepresentation();
        return ResponseEntity.ok(graph);
    }
}
