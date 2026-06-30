package br.ufpe.cin.taes2.korolev_engine;

import br.ufpe.cin.taes2.korolev_engine.controller.FeatureFlagController;
import br.ufpe.cin.taes2.korolev_engine.controller.GlobalExceptionHandler;
import br.ufpe.cin.taes2.korolev_engine.controller.UvlController;
import br.ufpe.cin.taes2.korolev_engine.controller.dto.ErrorResponse;
import br.ufpe.cin.taes2.korolev_engine.controller.dto.FeatureFlagRequest;
import br.ufpe.cin.taes2.korolev_engine.controller.dto.FeatureFlagResponse;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagAlreadyExistsException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.repository.FeatureFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KorolevEngineApplicationTests {

    @Autowired
    private FeatureFlagController controller;

    @Autowired
    private UvlController uvlController;

    @Autowired
    private FeatureFlagRepository repository;

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        repository.clear();
    }

    @Test
    void contextLoads() {
        assertNotNull(controller);
        assertNotNull(uvlController);
        assertNotNull(repository);
        assertNotNull(exceptionHandler);
    }

    @Test
    void shouldCreateRootFlagSuccessfully() {
        FeatureFlagRequest root = FeatureFlagRequest.builder()
                .name("App_UI_Platform")
                .active(true)
                .parentName(null)
                .build();

        ResponseEntity<FeatureFlagResponse> response = controller.createFlag(root);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("App_UI_Platform", response.getBody().getName());
        assertTrue(response.getBody().isActive());
    }

    @Test
    void shouldFailToCreateFlag_WhenFlagNameAlreadyExists() {
        FeatureFlagRequest first = FeatureFlagRequest.builder()
                .name("App_UI_Platform")
                .active(true)
                .parentName(null)
                .build();
        controller.createFlag(first);

        FeatureFlagRequest second = FeatureFlagRequest.builder()
                .name("App_UI_Platform")
                .active(true)
                .parentName(null)
                .build();

        FeatureFlagAlreadyExistsException ex = assertThrows(FeatureFlagAlreadyExistsException.class, () -> {
            controller.createFlag(second);
        });

        assertEquals("Feature flag already exists: App_UI_Platform", ex.getMessage());

        ResponseEntity<ErrorResponse> errorResponse = exceptionHandler.handleDomainException(ex);
        assertEquals(HttpStatus.CONFLICT, errorResponse.getStatusCode());
        assertNotNull(errorResponse.getBody());
        assertEquals("RESOURCE_ALREADY_EXISTS", errorResponse.getBody().getErrorCode());
    }

    @Test
    void shouldFailToCreateFlag_WhenCrossTreeConstraintViolated() {
        // Establish parent
        FeatureFlag parent = FeatureFlag.builder().name("App_UI_Platform").active(true).build();
        repository.save(parent);

        // Try to add Auto_Dark_Mode requiring Theme_Settings (not active or present)
        FeatureFlagRequest autoDarkMode = FeatureFlagRequest.builder()
                .name("Auto_Dark_Mode")
                .active(true)
                .parentName("App_UI_Platform")
                .requiresTarget("Theme_Settings")
                .build();

        FeatureFlagValidationException ex = assertThrows(FeatureFlagValidationException.class, () -> {
            controller.createFlag(autoDarkMode);
        });

        assertEquals("Erro na restrição cruzada de [Auto_Dark_Mode]: Requer que [Theme_Settings] esteja ATIVA.", ex.getMessage());

        ResponseEntity<ErrorResponse> errorResponse = exceptionHandler.handleDomainException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getStatusCode());
        assertNotNull(errorResponse.getBody());
        assertEquals("VALIDATION_ERROR", errorResponse.getBody().getErrorCode());
    }

    @Test
    void shouldUpdateFlagStatesBulk_AndResolveActivationDeadlock() {
        // 1. Create structure in inactive state (valid)
        FeatureFlag root = FeatureFlag.builder().name("App_UI_Platform").active(false).build();
        FeatureFlag child = FeatureFlag.builder().name("Theme_Settings").parentName("App_UI_Platform").mandatory(true).active(false).build();
        
        repository.save(root);
        repository.save(child);

        // 2. Perform bulk update mapping. Activating App_UI_Platform AND Theme_Settings together.
        // Doing this one-by-one is blocked due to validation constraints, but bulk update resolves it!
        ResponseEntity<Map<String, String>> response = controller.updateFlagStates(Map.of(
                "App_UI_Platform", true,
                "Theme_Settings", true
        ));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(repository.findByName("App_UI_Platform").get().isActive());
        assertTrue(repository.findByName("Theme_Settings").get().isActive());
    }

    @Test
    void shouldFailDelete_IfActiveChildDependsOnParent() {
        FeatureFlag root = FeatureFlag.builder().name("App_UI_Platform").active(true).build();
        FeatureFlag child = FeatureFlag.builder().name("Theme_Settings").parentName("App_UI_Platform").active(true).build();
        repository.save(root);
        repository.save(child);

        FeatureFlagValidationException exception = assertThrows(FeatureFlagValidationException.class, () -> {
            controller.deleteFlag("App_UI_Platform");
        });

        assertEquals("Erro de Hierarquia: A feature [Theme_Settings] está ativa, mas seu pai [App_UI_Platform] não está ativo.", exception.getMessage());
    }

    @Test
    void shouldImportAndExportUvlViaController() {
        String uvlInput = "features\n" +
                "    App_UI_Platform\n" +
                "        mandatory\n" +
                "            Theme_Settings\n";

        ResponseEntity<Map<String, String>> importResponse = uvlController.importUvl(uvlInput);
        assertEquals(HttpStatus.OK, importResponse.getStatusCode());
        assertNotNull(importResponse.getBody());

        ResponseEntity<String> exportResponse = uvlController.exportUvl();
        assertEquals(HttpStatus.OK, exportResponse.getStatusCode());
        assertNotNull(exportResponse.getBody());
        assertTrue(exportResponse.getBody().contains("App_UI_Platform"));
        assertTrue(exportResponse.getBody().contains("Theme_Settings"));
    }
}
