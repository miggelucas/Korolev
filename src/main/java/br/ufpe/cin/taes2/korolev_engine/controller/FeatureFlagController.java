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

import java.util.ArrayList;
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
            description = "Performs a bulk state update for the specified feature flags. All constraints are validated on the final configuration, resolving parent-child deadlocks. If override is true, the engine attempts to automatically resolve any constraint violations by toggling states in cascade."
    )
    @ApiResponse(responseCode = "200", description = "States updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation constraint violated by the proposed configuration")
    @ApiResponse(responseCode = "404", description = "One or more feature flags not found")
    public ResponseEntity<Map<String, String>> updateFlagStates(
            @RequestBody Map<String, Boolean> states,
            @RequestParam(defaultValue = "false") boolean override) {
        
        log.info("[FeatureFlagController] - Handle bulk state update request - PUT /api/flags/states for {} flags, override={}", states.size(), override);
        useCase.updateFlagStates(states, override);
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
            description = "Removes a feature flag if its deletion does not violate any hierarchical or cross-tree dependencies."
    )
    public ResponseEntity<Void> deleteFlag(@PathVariable String name) {
        log.info("[FeatureFlagController] - Handle delete request - DELETE /api/flags/{}", name);
        useCase.deleteFlag(name);
        return ResponseEntity.noContent().build();
    }

    private FeatureFlag toDomain(FeatureFlagRequest req) {
        return FeatureFlag.builder()
                .name(req.getName())
                .active(req.isActive())
                .parentName(req.getParentName())
                .mandatory(req.isMandatory())
                .requiresList(req.getRequiresList() != null ? req.getRequiresList() : new ArrayList<>())
                .excludesList(req.getExcludesList() != null ? req.getExcludesList() : new ArrayList<>())
                .build();
    }

    private FeatureFlagResponse toResponse(FeatureFlag flag) {
        return FeatureFlagResponse.builder()
                .name(flag.getName())
                .active(flag.isActive())
                .parentName(flag.getParentName())
                .mandatory(flag.isMandatory())
                .requiresList(flag.getRequiresList() != null ? flag.getRequiresList() : new ArrayList<>())
                .excludesList(flag.getExcludesList() != null ? flag.getExcludesList() : new ArrayList<>())
                .build();
    }
}
