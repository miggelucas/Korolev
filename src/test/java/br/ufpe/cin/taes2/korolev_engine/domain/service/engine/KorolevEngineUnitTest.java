package br.ufpe.cin.taes2.korolev_engine.domain.service.engine;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagAlreadyExistsException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagNotFoundException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KorolevEngineUnitTest {

    @Mock
    private ValidationRunner validationRunner;

    @Mock
    private AutoResolver autoResolver;

    private KorolevEngine engine;

    @BeforeEach
    void setUp() {
        engine = new KorolevEngine(validationRunner, autoResolver);
    }

    // ── Creation ──────────────────────────────────────────────────────

    @Test
    void shouldPassCreation_WhenFlagIsNewAndRulesPass() {
        FeatureFlag flag = FeatureFlag.builder().name("NewFlag").active(true).build();

        doNothing().when(validationRunner).validateGlobalState(any());

        assertDoesNotThrow(() -> engine.validateCreation(flag, Collections.emptyList()));
        verify(validationRunner, times(1)).validateGlobalState(any());
    }

    @Test
    void shouldFailCreation_WhenFlagAlreadyExists() {
        FeatureFlag existing = FeatureFlag.builder().name("Duplicate").active(true).build();
        FeatureFlag newFlag = FeatureFlag.builder().name("Duplicate").active(false).build();

        assertThrows(FeatureFlagAlreadyExistsException.class,
                () -> engine.validateCreation(newFlag, List.of(existing)));
    }

    @Test
    void shouldFailCreation_WhenValidationRunnerThrows() {
        FeatureFlag flag = FeatureFlag.builder().name("BadFlag").active(true).build();

        doThrow(new FeatureFlagValidationException("Erro", Collections.emptyList()))
                .when(validationRunner).validateGlobalState(any());

        assertThrows(FeatureFlagValidationException.class,
                () -> engine.validateCreation(flag, Collections.emptyList()));
    }

    // ── State Update ─────────────────────────────────────────────────

    @Test
    void shouldReturnUpdatedFlags_WhenStateUpdateIsValid() {
        FeatureFlag flag1 = FeatureFlag.builder().name("F1").active(false).build();
        FeatureFlag flag2 = FeatureFlag.builder().name("F2").active(false).build();

        when(validationRunner.executeValidation(any())).thenReturn(Collections.emptyList());

        List<FeatureFlag> result = engine.validateStateUpdate(
                Map.of("F1", true, "F2", true),
                List.of(flag1, flag2),
                false
        );

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(FeatureFlag::isActive));
        verify(autoResolver, never()).resolve(any(), any());
    }

    @Test
    void shouldFailStateUpdate_WhenFlagDoesNotExist() {
        FeatureFlag flag = FeatureFlag.builder().name("F1").active(false).build();

        assertThrows(FeatureFlagNotFoundException.class,
                () -> engine.validateStateUpdate(Map.of("F1", true, "Ghost", true), List.of(flag), false));
    }

    // ── Auto-Resolution (Override) ───────────────────────────────────

    @Test
    void shouldDelegateToAutoResolver_WhenErrorsExistAndOverrideIsTrue() {
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(true).build();
        ValidationError err = ValidationError.builder().type(ErrorType.HIERARCHY).build();

        when(validationRunner.executeValidation(any())).thenReturn(List.of(err));
        doNothing().when(autoResolver).resolve(anyMap(), anyList());

        List<FeatureFlag> result = engine.validateStateUpdate(
                Map.of("Parent", false),
                List.of(parent),
                true
        );

        assertNotNull(result);
        verify(autoResolver, times(1)).resolve(anyMap(), eq(List.of(err)));
    }

    @Test
    void shouldThrowException_WhenErrorsExistAndOverrideIsFalse() {
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(true).build();
        ValidationError err = ValidationError.builder().type(ErrorType.HIERARCHY).build();

        when(validationRunner.executeValidation(any())).thenReturn(List.of(err));

        assertThrows(FeatureFlagValidationException.class, () -> 
            engine.validateStateUpdate(Map.of("Parent", false), List.of(parent), false)
        );
        
        verify(autoResolver, never()).resolve(anyMap(), anyList());
    }

    // ── Deletion ─────────────────────────────────────────────────────

    @Test
    void shouldPassDeletion_WhenRemainingStateIsValid() {
        FeatureFlag target = FeatureFlag.builder().name("Target").active(true).build();
        doNothing().when(validationRunner).validateGlobalState(any());

        assertDoesNotThrow(() -> engine.validateDeletion("Target", List.of(target)));
    }

    @Test
    void shouldFailDeletion_WhenRemainingStateIsInvalid() {
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(true).build();
        
        doThrow(new FeatureFlagValidationException("Erro", Collections.emptyList()))
            .when(validationRunner).validateGlobalState(any());

        assertThrows(FeatureFlagValidationException.class,
                () -> engine.validateDeletion("Parent", List.of(parent)));
    }

    @Test
    void shouldDoNothing_WhenDeletingNonExistentFlag() {
        FeatureFlag existing = FeatureFlag.builder().name("Existing").active(true).build();
        assertDoesNotThrow(() -> engine.validateDeletion("Ghost", List.of(existing)));
    }
}
