package br.ufpe.cin.taes2.korolev_engine.controller;

import br.ufpe.cin.taes2.korolev_engine.application.usecase.FeatureFlagUseCase;
import br.ufpe.cin.taes2.korolev_engine.controller.dto.FeatureFlagRequest;
import br.ufpe.cin.taes2.korolev_engine.controller.dto.FeatureFlagResponse;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/flags")
@RequiredArgsConstructor
@Tag(name = "Feature Flags", description = "CRUD operations and graph visualization for feature flags with SPL variability validation")
public class FeatureFlagController {

    private final FeatureFlagUseCase useCase;

    @PostMapping
    @Operation(
            summary = "Create a new feature flag",
            description = "Registers a new feature flag. Validates it against all SPL model constraints. Throws a 409 Conflict if the flag already exists."
    )
    @ApiResponse(responseCode = "200", description = "Flag created successfully", content = @Content(schema = @Schema(implementation = FeatureFlagResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation constraint violated")
    @ApiResponse(responseCode = "409", description = "Feature flag already exists")
    public ResponseEntity<FeatureFlagResponse> createFlag(@RequestBody FeatureFlagRequest request) {
        log.info("[FeatureFlagController] - Handle create request - POST /api/flags for name={}, active={}", request.getName(), request.isActive());
        FeatureFlag domainFlag = toDomain(request);
        useCase.createFlag(domainFlag);
        return ResponseEntity.ok(toResponse(domainFlag));
    }
    
    @PutMapping("/states")
    @Operation(
            summary = "Update multiple feature flag states",
            description = "Performs a bulk state update for the specified feature flags. All constraints are validated on the final configuration, resolving parent-child deadlocks."
    )
    @ApiResponse(responseCode = "200", description = "States updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation constraint violated by the proposed configuration")
    @ApiResponse(responseCode = "404", description = "One or more feature flags not found")
    public ResponseEntity<Map<String, String>> updateFlagStates(@RequestBody Map<String, Boolean> states) {
        log.info("[FeatureFlagController] - Handle bulk state update request - PUT /api/flags/states for {} flags", states.size());
        useCase.updateFlagStates(states);
        return ResponseEntity.ok(Map.of("message", "Estados atualizados com sucesso."));
    }

    @GetMapping
    @Operation(summary = "List all feature flags", description = "Returns all registered feature flags with their current state and constraints.")
    public ResponseEntity<List<FeatureFlagResponse>> getAllFlags() {
        log.info("[FeatureFlagController] - Handle list request - GET /api/flags");
        List<FeatureFlagResponse> responses = useCase.getAllFlags().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        log.debug("[FeatureFlagController] - Handle list response - GET /api/flags returned {} items", responses.size());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get a feature flag by name", description = "Returns a single feature flag identified by its unique name.")
    @ApiResponse(responseCode = "200", description = "Flag found")
    @ApiResponse(responseCode = "404", description = "Flag not found")
    public ResponseEntity<FeatureFlagResponse> getFlagByName(@PathVariable String name) {
        log.info("[FeatureFlagController] - Handle get by name request - GET /api/flags/{}", name);
        return useCase.getFlagByName(name)
                .map(flag -> {
                    log.debug("[FeatureFlagController] - Handle get by name response - GET /api/flags/{} found", name);
                    return ResponseEntity.ok(toResponse(flag));
                })
                .orElseGet(() -> {
                    log.warn("[FeatureFlagController] - Handle get by name response - GET /api/flags/{} NOT found", name);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{name}")
    @Operation(
            summary = "Delete a feature flag",
            description = "Validates that removing this flag does not break any active dependency before deleting. Returns HTTP 400 if deletion would violate constraints."
    )
    @ApiResponse(responseCode = "200", description = "Flag deleted successfully")
    @ApiResponse(responseCode = "400", description = "Deletion would break active dependencies")
    public ResponseEntity<Map<String, String>> deleteFlag(@PathVariable String name) {
        log.info("[FeatureFlagController] - Handle delete request - DELETE /api/flags/{}", name);
        useCase.deleteFlag(name);
        return ResponseEntity.ok(Map.of("message", "Flag removida com sucesso."));
    }

    @GetMapping(value = "/graph", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "Render the feature flag dependency graph",
            description = "Returns an ASCII tree visualization of all feature flags showing their hierarchy, activation status, and constraint annotations."
    )
    public ResponseEntity<String> getGraph() {
        log.info("[FeatureFlagController] - Handle render graph request - GET /api/flags/graph");
        String graph = useCase.getGraphRepresentation();
        return ResponseEntity.ok(graph);
    }

    private FeatureFlag toDomain(FeatureFlagRequest req) {
        return FeatureFlag.builder()
                .name(req.getName())
                .active(req.isActive())
                .parentName(req.getParentName())
                .mandatory(req.isMandatory())
                .requiresTarget(req.getRequiresTarget())
                .excludesList(req.getExcludesList() != null ? req.getExcludesList() : List.of())
                .build();
    }

    private FeatureFlagResponse toResponse(FeatureFlag flag) {
        return FeatureFlagResponse.builder()
                .name(flag.getName())
                .active(flag.isActive())
                .parentName(flag.getParentName())
                .mandatory(flag.isMandatory())
                .requiresTarget(flag.getRequiresTarget())
                .excludesList(flag.getExcludesList())
                .build();
    }
}
