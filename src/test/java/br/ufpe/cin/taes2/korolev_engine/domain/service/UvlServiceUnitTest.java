package br.ufpe.cin.taes2.korolev_engine.domain.service;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.repository.InMemoryFeatureFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UvlServiceUnitTest {

    private InMemoryFeatureFlagRepository repository;
    private KorolevEngine korolevEngine;
    private UvlService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryFeatureFlagRepository();
        korolevEngine = mock(KorolevEngine.class);
        service = new UvlService(repository, korolevEngine);
    }

    @Test
    void shouldImportValidUvlTree() {
        String uvl = "features\n" +
                "    App_UI_Platform\n" +
                "        mandatory\n" +
                "            Theme_Settings\n" +
                "                optional\n" +
                "                    Auto_Dark_Mode\n" +
                "        optional\n" +
                "            Theme_Customizer\n" +
                "                alternative\n" +
                "                    Light_Theme\n" +
                "                    Dark_Theme\n" +
                "constraints\n" +
                "    Auto_Dark_Mode requires Theme_Settings\n" +
                "    Dark_Theme excludes Light_Theme\n";

        // Assert import doesn't throw and calls validation engine
        assertDoesNotThrow(() -> service.importUvl(uvl));
        verify(korolevEngine, times(1)).validateGlobalState(anyList());

        // Check stored flags
        List<FeatureFlag> flags = repository.findAll();
        assertEquals(6, flags.size());

        // 1. Root
        Optional<FeatureFlag> root = repository.findByName("App_UI_Platform");
        assertTrue(root.isPresent());
        assertNull(root.get().getParentName());
        assertFalse(root.get().isMandatory());

        // 2. Mandatory Child
        Optional<FeatureFlag> settings = repository.findByName("Theme_Settings");
        assertTrue(settings.isPresent());
        assertEquals("App_UI_Platform", settings.get().getParentName());
        assertTrue(settings.get().isMandatory());

        // 3. Optional Child of child
        Optional<FeatureFlag> autoDark = repository.findByName("Auto_Dark_Mode");
        assertTrue(autoDark.isPresent());
        assertEquals("Theme_Settings", autoDark.get().getParentName());
        assertFalse(autoDark.get().isMandatory());
        assertEquals("Theme_Settings", autoDark.get().getRequiresTarget());

        // 4. Alternative siblings
        Optional<FeatureFlag> light = repository.findByName("Light_Theme");
        Optional<FeatureFlag> dark = repository.findByName("Dark_Theme");
        assertTrue(light.isPresent());
        assertTrue(dark.isPresent());
        
        assertEquals("Theme_Customizer", light.get().getParentName());
        assertEquals("Theme_Customizer", dark.get().getParentName());
        assertFalse(light.get().isMandatory());
        assertFalse(dark.get().isMandatory());
        
        // Alternative semantics: siblings must exclude each other
        assertTrue(light.get().getExcludesList().contains("Dark_Theme"));
        assertTrue(dark.get().getExcludesList().contains("Light_Theme"));
    }

    @Test
    void shouldExportFlagsToUvlFormat() {
        // Setup a repository state
        FeatureFlag root = FeatureFlag.builder().name("Root").parentName(null).active(true).build();
        FeatureFlag child1 = FeatureFlag.builder().name("MandatoryChild").parentName("Root").mandatory(true).active(true).build();
        FeatureFlag child2 = FeatureFlag.builder().name("OptionalChild").parentName("Root").mandatory(false).active(false).requiresTarget("MandatoryChild").build();
        
        repository.save(root);
        repository.save(child1);
        repository.save(child2);

        String uvl = service.exportUvl();
        
        assertTrue(uvl.contains("features"));
        assertTrue(uvl.contains("Root"));
        assertTrue(uvl.contains("mandatory"));
        assertTrue(uvl.contains("MandatoryChild"));
        assertTrue(uvl.contains("optional"));
        assertTrue(uvl.contains("OptionalChild"));
        assertTrue(uvl.contains("constraints"));
        assertTrue(uvl.contains("OptionalChild requires MandatoryChild"));
    }
}
