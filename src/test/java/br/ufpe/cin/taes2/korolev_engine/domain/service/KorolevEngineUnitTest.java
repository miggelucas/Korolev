package br.ufpe.cin.taes2.korolev_engine.domain.service;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagAlreadyExistsException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagNotFoundException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KorolevEngineUnitTest {

    @Mock
    private ValidationRule rule1;

    @Mock
    private ValidationRule rule2;

    private KorolevEngine engine;

    @BeforeEach
    void setUp() {
        engine = new KorolevEngine(Arrays.asList(rule1, rule2));
    }

    // ── Creation ──────────────────────────────────────────────────────

    @Test
    void shouldPassCreation_WhenFlagIsNewAndRulesPass() {
        FeatureFlag flag = FeatureFlag.builder().name("NewFlag").active(true).build();

        assertDoesNotThrow(() -> engine.validateCreation(flag, Collections.emptyList()));

        verify(rule1, times(1)).validate(eq(flag), anyMap());
        verify(rule2, times(1)).validate(eq(flag), anyMap());
    }

    @Test
    void shouldFailCreation_WhenFlagAlreadyExists() {
        FeatureFlag existing = FeatureFlag.builder().name("Duplicate").active(true).build();
        FeatureFlag newFlag = FeatureFlag.builder().name("Duplicate").active(false).build();

        assertThrows(FeatureFlagAlreadyExistsException.class,
                () -> engine.validateCreation(newFlag, List.of(existing)));
    }

    @Test
    void shouldFailCreation_WhenValidationRuleFails() {
        FeatureFlag flag = FeatureFlag.builder().name("BadFlag").active(true).build();

        doThrow(new FeatureFlagValidationException("Validation Failed"))
                .when(rule1).validate(eq(flag), anyMap());

        assertThrows(FeatureFlagValidationException.class,
                () -> engine.validateCreation(flag, Collections.emptyList()));
    }

    // ── State Update ─────────────────────────────────────────────────

    @Test
    void shouldReturnUpdatedFlags_WhenStateUpdateIsValid() {
        FeatureFlag flag1 = FeatureFlag.builder().name("F1").active(false).parentName(null).build();
        FeatureFlag flag2 = FeatureFlag.builder().name("F2").active(false).parentName("F1").build();

        List<FeatureFlag> result = engine.validateStateUpdate(
                Map.of("F1", true, "F2", true),
                List.of(flag1, flag2)
        );

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(FeatureFlag::isActive));
    }

    @Test
    void shouldFailStateUpdate_WhenFlagDoesNotExist() {
        FeatureFlag flag = FeatureFlag.builder().name("F1").active(false).build();

        assertThrows(FeatureFlagNotFoundException.class,
                () -> engine.validateStateUpdate(Map.of("F1", true, "Ghost", true), List.of(flag)));
    }

    // ── Deletion ─────────────────────────────────────────────────────

    @Test
    void shouldPassDeletion_WhenRemainingStateIsValid() {
        FeatureFlag target = FeatureFlag.builder().name("Target").active(true).build();

        assertDoesNotThrow(() -> engine.validateDeletion("Target", List.of(target)));
    }

    @Test
    void shouldFailDeletion_WhenRemainingStateIsInvalid() {
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(true).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(true).parentName("Parent").build();

        doThrow(new FeatureFlagValidationException("Broken hierarchy"))
                .when(rule1).validate(eq(child), anyMap());

        assertThrows(FeatureFlagValidationException.class,
                () -> engine.validateDeletion("Parent", List.of(parent, child)));
    }

    @Test
    void shouldDoNothing_WhenDeletingNonExistentFlag() {
        FeatureFlag existing = FeatureFlag.builder().name("Existing").active(true).build();

        assertDoesNotThrow(() -> engine.validateDeletion("Ghost", List.of(existing)));
    }
}
