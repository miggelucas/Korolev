package br.ufpe.cin.taes2.korolev_engine.controller;

import br.ufpe.cin.taes2.korolev_engine.application.usecase.UvlUseCase;
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

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/flags/uvl")
@RequiredArgsConstructor
@Tag(name = "Variability Models (UVL)", description = "Import and export variability models in UVL (Universal Variability Language) format")
public class UvlController {

    private final UvlUseCase useCase;

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "Import variability model from UVL format",
            description = "Parses and imports a variability model in Universal Variability Language (UVL) format, replacing the current feature flag configuration. Validates structural consistency before saving."
    )
    @ApiResponse(responseCode = "200", description = "UVL model imported successfully")
    @ApiResponse(responseCode = "400", description = "Parsing error or model validation failed", content = @Content(schema = @Schema(implementation = Map.class)))
    public ResponseEntity<Map<String, String>> importUvl(@RequestBody String uvlContent) {
        log.info("[UvlController] - Handle import UVL request - POST /api/flags/uvl");
        useCase.importUvl(uvlContent);
        return ResponseEntity.ok(Map.of("message", "Modelo UVL importado com sucesso."));
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "Export configuration in UVL format",
            description = "Serializes the current feature flag configuration and cross-tree constraints into Universal Variability Language (UVL) format."
    )
    public ResponseEntity<String> exportUvl() {
        log.info("[UvlController] - Handle export UVL request - GET /api/flags/uvl");
        String uvl = useCase.exportUvl();
        return ResponseEntity.ok(uvl);
    }
}
