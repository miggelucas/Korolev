package br.ufpe.cin.taes2.korolev_engine;

import br.ufpe.cin.taes2.korolev_engine.controller.FeatureFlagController;
import br.ufpe.cin.taes2.korolev_engine.controller.GlobalExceptionHandler;
import br.ufpe.cin.taes2.korolev_engine.controller.UvlController;
import br.ufpe.cin.taes2.korolev_engine.controller.dto.ErrorResponse;
import br.ufpe.cin.taes2.korolev_engine.controller.dto.FeatureFlagRequest;
import br.ufpe.cin.taes2.korolev_engine.controller.dto.FeatureFlagResponse;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagAlreadyExistsException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagNotFoundException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.repository.FeatureFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    @DisplayName("Spring context loads all beans successfully")
    void contextLoads() {
        assertNotNull(controller);
        assertNotNull(uvlController);
        assertNotNull(repository);
        assertNotNull(exceptionHandler);
    }

    // ══════════════════════════════════════════════════════════════════
    //  CREATION
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Flag Creation (POST /api/flags)")
    class FlagCreation {

        @Test
        @DisplayName("Should create a root flag successfully when no constraints are violated")
        void shouldCreateRootFlagSuccessfully() {
            FeatureFlagRequest root = FeatureFlagRequest.builder()
                    .name("App_UI_Platform")
                    .active(true)
                    .build();

            ResponseEntity<FeatureFlagResponse> response = controller.createFlag(root);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("App_UI_Platform", response.getBody().getName());
            assertTrue(response.getBody().isActive());
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when creating a flag with a name that already exists")
        void shouldFailWhenFlagNameAlreadyExists() {
            controller.createFlag(FeatureFlagRequest.builder().name("Root").active(true).build());

            FeatureFlagAlreadyExistsException ex = assertThrows(
                    FeatureFlagAlreadyExistsException.class,
                    () -> controller.createFlag(FeatureFlagRequest.builder().name("Root").active(true).build())
            );

            ResponseEntity<ErrorResponse> error = exceptionHandler.handleDomainException(ex);
            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
            assertEquals("RESOURCE_ALREADY_EXISTS", error.getBody().getErrorCode());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  VALIDATION RULES
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("HierarchyRule — An active child requires its parent to be active")
    class HierarchyRuleTests {

        @Test
        @DisplayName("Should reject creating an active child flag when its parent is inactive")
        void shouldRejectActiveChildWithInactiveParent() {
            // Parent exists but is INACTIVE
            repository.save(FeatureFlag.builder().name("App_UI_Platform").active(false).build());

            // Try to create an ACTIVE child under the inactive parent
            FeatureFlagRequest child = FeatureFlagRequest.builder()
                    .name("Theme_Settings")
                    .active(true)
                    .parentName("App_UI_Platform")
                    .build();

            FeatureFlagValidationException ex = assertThrows(
                    FeatureFlagValidationException.class,
                    () -> controller.createFlag(child)
            );

            assertTrue(ex.getMessage().contains("Erro de Hierarquia"));
            assertTrue(ex.getMessage().contains("Theme_Settings"));
            assertTrue(ex.getMessage().contains("App_UI_Platform"));
        }

        @Test
        @DisplayName("Should reject deleting a parent flag when it has active children")
        void shouldRejectDeletingParentWithActiveChildren() {
            repository.save(FeatureFlag.builder().name("App_UI_Platform").active(true).build());
            repository.save(FeatureFlag.builder().name("Theme_Settings").active(true).parentName("App_UI_Platform").build());

            FeatureFlagValidationException ex = assertThrows(
                    FeatureFlagValidationException.class,
                    () -> controller.deleteFlag("App_UI_Platform")
            );

            assertTrue(ex.getMessage().contains("Erro de Hierarquia"));
        }
    }

    @Nested
    @DisplayName("MandatoryRule — An active parent requires its mandatory children to be active")
    class MandatoryRuleTests {

        @Test
        @DisplayName("Should reject activating a parent without activating its mandatory child")
        void shouldRejectActivatingParentWithoutMandatoryChild() {
            // Setup: parent and mandatory child both inactive
            repository.save(FeatureFlag.builder().name("App_UI_Platform").active(false).build());
            repository.save(FeatureFlag.builder().name("Theme_Settings").active(false)
                    .parentName("App_UI_Platform").mandatory(true).build());

            // Try to activate ONLY the parent (leaving the mandatory child inactive)
            FeatureFlagValidationException ex = assertThrows(
                    FeatureFlagValidationException.class,
                    () -> controller.updateFlagStates(Map.of("App_UI_Platform", true))
            );

            assertTrue(ex.getMessage().contains("Erro de Mandatoriedade"));
            assertTrue(ex.getMessage().contains("Theme_Settings"));
        }

        @Test
        @DisplayName("Should allow activating parent AND mandatory child together via bulk update")
        void shouldAllowActivatingParentAndMandatoryChildTogether() {
            repository.save(FeatureFlag.builder().name("App_UI_Platform").active(false).build());
            repository.save(FeatureFlag.builder().name("Theme_Settings").active(false)
                    .parentName("App_UI_Platform").mandatory(true).build());

            // Activate both at once — resolves the chicken-and-egg deadlock
            ResponseEntity<Map<String, String>> response = controller.updateFlagStates(
                    Map.of("App_UI_Platform", true, "Theme_Settings", true)
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(repository.findByName("App_UI_Platform").get().isActive());
            assertTrue(repository.findByName("Theme_Settings").get().isActive());
        }
    }

    @Nested
    @DisplayName("CrossTreeRequiresRule — A flag requiring another flag needs the target to be active")
    class CrossTreeRequiresRuleTests {

        @Test
        @DisplayName("Should reject creating an active flag that requires an inactive target")
        void shouldRejectFlagRequiringInactiveTarget() {
            repository.save(FeatureFlag.builder().name("App_UI_Platform").active(true).build());
            repository.save(FeatureFlag.builder().name("Theme_Settings").active(false)
                    .parentName("App_UI_Platform").build());

            // Auto_Dark_Mode requires Theme_Settings to be active
            FeatureFlagRequest autoDarkMode = FeatureFlagRequest.builder()
                    .name("Auto_Dark_Mode")
                    .active(true)
                    .parentName("App_UI_Platform")
                    .requiresTarget("Theme_Settings")
                    .build();

            FeatureFlagValidationException ex = assertThrows(
                    FeatureFlagValidationException.class,
                    () -> controller.createFlag(autoDarkMode)
            );

            assertTrue(ex.getMessage().contains("restrição cruzada"));
            assertTrue(ex.getMessage().contains("Auto_Dark_Mode"));
            assertTrue(ex.getMessage().contains("Theme_Settings"));
        }
    }

    @Nested
    @DisplayName("MutualExclusionRule — Two mutually exclusive flags cannot both be active")
    class MutualExclusionRuleTests {

        @Test
        @DisplayName("Should reject activating a flag that is mutually exclusive with an already active flag")
        void shouldRejectActivatingMutuallyExclusiveFlags() {
            // Setup: parent active, Light_Theme active with excludes Dark_Theme
            repository.save(FeatureFlag.builder().name("App_UI_Platform").active(true).build());
            repository.save(FeatureFlag.builder().name("Light_Theme").active(true)
                    .parentName("App_UI_Platform")
                    .excludesList(List.of("Dark_Theme")).build());

            // Try to create Dark_Theme as active — conflicts with Light_Theme's excludesList
            FeatureFlagRequest darkTheme = FeatureFlagRequest.builder()
                    .name("Dark_Theme")
                    .active(true)
                    .parentName("App_UI_Platform")
                    .excludesList(List.of("Light_Theme"))
                    .build();

            FeatureFlagValidationException ex = assertThrows(
                    FeatureFlagValidationException.class,
                    () -> controller.createFlag(darkTheme)
            );

            assertTrue(ex.getMessage().contains("Exclusividade Mútua"));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  BULK STATE UPDATE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Bulk State Update (PUT /api/flags/states)")
    class BulkStateUpdateTests {

        @Test
        @DisplayName("Should return 404 NOT FOUND when updating a flag that does not exist")
        void shouldRejectUpdateForNonExistentFlag() {
            repository.save(FeatureFlag.builder().name("Existing").active(false).build());

            FeatureFlagNotFoundException ex = assertThrows(
                    FeatureFlagNotFoundException.class,
                    () -> controller.updateFlagStates(Map.of("Existing", true, "Ghost", true))
            );

            ResponseEntity<ErrorResponse> error = exceptionHandler.handleDomainException(ex);
            assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
            assertEquals("RESOURCE_NOT_FOUND", error.getBody().getErrorCode());

            // Existing flag should NOT have been updated (transaction-like behavior)
            assertFalse(repository.findByName("Existing").get().isActive());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  UVL IMPORT / EXPORT
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("UVL Import/Export (POST & GET /api/flags/uvl)")
    class UvlTests {

        @Test
        @DisplayName("Should import a UVL model and export it back preserving the structure")
        void shouldImportAndExportUvlRoundTrip() {
            String uvlInput = "features\n" +
                    "    App_UI_Platform\n" +
                    "        mandatory\n" +
                    "            Theme_Settings\n";

            ResponseEntity<Map<String, String>> importResponse = uvlController.importUvl(uvlInput);
            assertEquals(HttpStatus.OK, importResponse.getStatusCode());

            ResponseEntity<String> exportResponse = uvlController.exportUvl();
            assertEquals(HttpStatus.OK, exportResponse.getStatusCode());
            assertTrue(exportResponse.getBody().contains("App_UI_Platform"));
            assertTrue(exportResponse.getBody().contains("Theme_Settings"));
        }
    }
}
