package br.ufpe.cin.taes2.korolev_engine.domain.validation;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.rules.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRulesUnitTest {

    @Test
    @DisplayName("SemanticRule should fail if a flag requires itself")
    void testSemanticRuleSelfRequirement() {
        SemanticValidationRule rule = new SemanticValidationRule();
        FeatureFlag flag = FeatureFlag.builder().name("SelfFlag").requiresList(List.of("SelfFlag")).build();
        Map<String, FeatureFlag> state = new HashMap<>();
        
        List<ValidationError> errors = rule.validate(flag, state);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).getMessage().contains("não pode requerer a si mesma"));
    }

    @Test
    @DisplayName("SemanticRule should fail if a flag requires its parent")
    void testSemanticRuleParentRequirement() {
        SemanticValidationRule rule = new SemanticValidationRule();
        FeatureFlag flag = FeatureFlag.builder().name("ChildFlag").parentName("ParentFlag").requiresList(List.of("ParentFlag")).build();
        Map<String, FeatureFlag> state = new HashMap<>();
        
        List<ValidationError> errors = rule.validate(flag, state);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).getMessage().contains("dependência já é garantida pela hierarquia"));
    }

    // --- HierarchyRule Tests ---

    @Test
    void hierarchyRule_shouldPass_whenParentAndChildActive() {
        HierarchyRule rule = new HierarchyRule();
        
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(true).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(true).parentName("Parent").build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Parent", parent);
        state.put("Child", child);

        assertTrue(rule.validate(child, state).isEmpty());
    }

    @Test
    void hierarchyRule_shouldPass_whenChildInactive() {
        HierarchyRule rule = new HierarchyRule();
        
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(false).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(false).parentName("Parent").build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Parent", parent);
        state.put("Child", child);

        assertTrue(rule.validate(child, state).isEmpty());
    }

    @Test
    void hierarchyRule_shouldFail_whenParentInactive() {
        HierarchyRule rule = new HierarchyRule();
        
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(false).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(true).parentName("Parent").build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Parent", parent);
        state.put("Child", child);

        assertFalse(rule.validate(child, state).isEmpty());
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

        assertTrue(rule.validate(child, state).isEmpty());
    }

    @Test
    void mandatoryRule_shouldFail_whenParentActiveAndChildInactive() {
        MandatoryRule rule = new MandatoryRule();
        
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(true).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(false).parentName("Parent").mandatory(true).build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Parent", parent);
        state.put("Child", child);

        assertFalse(rule.validate(child, state).isEmpty());
    }

    // --- CrossTreeRequiresRule Tests ---

    @Test
    void crossTreeRequiresRule_shouldPass_whenTargetActive() {
        CrossTreeRequiresRule rule = new CrossTreeRequiresRule();
        
        FeatureFlag target = FeatureFlag.builder().name("Target").active(true).build();
        FeatureFlag flag = FeatureFlag.builder().name("Flag").active(true).requiresList(List.of("Target")).build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Target", target);
        state.put("Flag", flag);

        assertTrue(rule.validate(flag, state).isEmpty());
    }

    @Test
    void crossTreeRequiresRule_shouldFail_whenTargetInactiveOrMissing() {
        CrossTreeRequiresRule rule = new CrossTreeRequiresRule();
        
        FeatureFlag target = FeatureFlag.builder().name("Target").active(false).build();
        FeatureFlag flag = FeatureFlag.builder().name("Flag").active(true).requiresList(List.of("Target")).build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("Target", target);
        state.put("Flag", flag);

        assertFalse(rule.validate(flag, state).isEmpty());
        
        // Missing target
        state.remove("Target");
        assertFalse(rule.validate(flag, state).isEmpty());
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

        assertTrue(rule.validate(flag, state).isEmpty());
    }

    @Test
    void mutualExclusionRule_shouldFail_whenExcludedFlagActive() {
        MutualExclusionRule rule = new MutualExclusionRule();
        
        FeatureFlag excluded = FeatureFlag.builder().name("B").active(true).build();
        FeatureFlag flag = FeatureFlag.builder().name("A").active(true).excludesList(List.of("B")).build();

        Map<String, FeatureFlag> state = new HashMap<>();
        state.put("B", excluded);
        state.put("A", flag);

        assertFalse(rule.validate(flag, state).isEmpty());
    }
}
