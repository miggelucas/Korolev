package br.ufpe.cin.taes2.korolev_engine.domain.validation;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRulesUnitTest {

    // --- HierarchyRule Tests ---

    @Test
    void hierarchyRule_shouldPass_whenParentAndChildActive() {
        HierarchyRule rule = new HierarchyRule();
        
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(true).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(true).parentName("Parent").build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Parent", parent);
        state.put("Child", child);

        assertDoesNotThrow(() -> rule.validate(child, state));
    }

    @Test
    void hierarchyRule_shouldPass_whenChildInactive() {
        HierarchyRule rule = new HierarchyRule();
        
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(false).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(false).parentName("Parent").build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Parent", parent);
        state.put("Child", child);

        assertDoesNotThrow(() -> rule.validate(child, state));
    }

    @Test
    void hierarchyRule_shouldFail_whenParentInactive() {
        HierarchyRule rule = new HierarchyRule();
        
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(false).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(true).parentName("Parent").build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Parent", parent);
        state.put("Child", child);

        assertThrows(FeatureFlagValidationException.class, () -> rule.validate(child, state));
    }

    // --- MandatoryRule Tests ---

    @Test
    void mandatoryRule_shouldPass_whenParentActiveAndChildActive() {
        MandatoryRule rule = new MandatoryRule();
        
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(true).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(true).parentName("Parent").mandatory(true).build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Parent", parent);
        state.put("Child", child);

        assertDoesNotThrow(() -> rule.validate(child, state));
    }

    @Test
    void mandatoryRule_shouldFail_whenParentActiveAndChildInactive() {
        MandatoryRule rule = new MandatoryRule();
        
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(true).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(false).parentName("Parent").mandatory(true).build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Parent", parent);
        state.put("Child", child);

        assertThrows(FeatureFlagValidationException.class, () -> rule.validate(child, state));
    }

    // --- CrossTreeRequiresRule Tests ---

    @Test
    void crossTreeRequiresRule_shouldPass_whenTargetActive() {
        CrossTreeRequiresRule rule = new CrossTreeRequiresRule();
        
        FeatureFlag target = FeatureFlag.builder().name("Target").active(true).build();
        FeatureFlag flag = FeatureFlag.builder().name("Flag").active(true).requiresTarget("Target").build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Target", target);
        state.put("Flag", flag);

        assertDoesNotThrow(() -> rule.validate(flag, state));
    }

    @Test
    void crossTreeRequiresRule_shouldFail_whenTargetInactiveOrMissing() {
        CrossTreeRequiresRule rule = new CrossTreeRequiresRule();
        
        FeatureFlag target = FeatureFlag.builder().name("Target").active(false).build();
        FeatureFlag flag = FeatureFlag.builder().name("Flag").active(true).requiresTarget("Target").build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Target", target);
        state.put("Flag", flag);

        assertThrows(FeatureFlagValidationException.class, () -> rule.validate(flag, state));
        
        // Missing target
        state.remove("Target");
        assertThrows(FeatureFlagValidationException.class, () -> rule.validate(flag, state));
    }

    // --- MutualExclusionRule Tests ---

    @Test
    void mutualExclusionRule_shouldPass_whenExcludedFlagInactive() {
        MutualExclusionRule rule = new MutualExclusionRule();
        
        FeatureFlag excluded = FeatureFlag.builder().name("B").active(false).build();
        FeatureFlag flag = FeatureFlag.builder().name("A").active(true).excludesList(List.of("B")).build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("B", excluded);
        state.put("A", flag);

        assertDoesNotThrow(() -> rule.validate(flag, state));
    }

    @Test
    void mutualExclusionRule_shouldFail_whenExcludedFlagActive() {
        MutualExclusionRule rule = new MutualExclusionRule();
        
        FeatureFlag excluded = FeatureFlag.builder().name("B").active(true).build();
        FeatureFlag flag = FeatureFlag.builder().name("A").active(true).excludesList(List.of("B")).build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("B", excluded);
        state.put("A", flag);

        assertThrows(FeatureFlagValidationException.class, () -> rule.validate(flag, state));
    }
}
