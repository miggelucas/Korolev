package br.ufpe.cin.taes2.korolev_engine.domain.service;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UvlServiceUnitTest {

    private UvlService service;

    @BeforeEach
    void setUp() {
        service = new UvlService();
    }

    @Test
    void shouldImportValidUvlTree() {
        String uvl = """
                features
                    App_UI_Platform
                        mandatory
                            Theme_Settings
                                optional
                                    Auto_Dark_Mode
                        optional
                            Theme_Customizer
                                alternative
                                    Light_Theme
                                    Dark_Theme
                constraints
                    Auto_Dark_Mode requires Theme_Settings
                    Dark_Theme excludes Light_Theme
                """;

        List<FeatureFlag> flags = assertDoesNotThrow(() -> service.parseUvl(uvl));
        assertEquals(6, flags.size());

        // 1. Root
        Optional<FeatureFlag> root = flags.stream().filter(f -> f.getName().equals("App_UI_Platform")).findFirst();
        assertTrue(root.isPresent());
        assertNull(root.get().getParentName());
        assertFalse(root.get().isMandatory());

        // 2. Mandatory Child
        Optional<FeatureFlag> settings = flags.stream().filter(f -> f.getName().equals("Theme_Settings")).findFirst();
        assertTrue(settings.isPresent());
        assertEquals("App_UI_Platform", settings.get().getParentName());
        assertTrue(settings.get().isMandatory());

        // 3. Optional Child of child
        Optional<FeatureFlag> autoDark = flags.stream().filter(f -> f.getName().equals("Auto_Dark_Mode")).findFirst();
        assertTrue(autoDark.isPresent());
        assertEquals("Theme_Settings", autoDark.get().getParentName());
        assertFalse(autoDark.get().isMandatory());
        assertTrue(autoDark.get().getRequiresList().contains("Theme_Settings"));

        // 4. Alternative siblings
        Optional<FeatureFlag> light = flags.stream().filter(f -> f.getName().equals("Light_Theme")).findFirst();
        Optional<FeatureFlag> dark = flags.stream().filter(f -> f.getName().equals("Dark_Theme")).findFirst();
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
        FeatureFlag root = FeatureFlag.builder().name("Root").parentName(null).active(true).build();
        FeatureFlag child1 = FeatureFlag.builder().name("MandatoryChild").parentName("Root").mandatory(true).active(true).build();
        FeatureFlag child2 = FeatureFlag.builder().name("OptionalChild").parentName("Root").mandatory(false).active(false).requiresList(List.of("MandatoryChild")).build();

        List<FeatureFlag> flags = List.of(root, child1, child2);

        String uvl = service.exportUvl(flags);

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
