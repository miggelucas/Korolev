package br.ufpe.cin.taes2.korolev_engine.domain.service.engine;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ErrorType;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoResolverUnitTest {

    @Mock
    private ValidationRunner validationRunner;

    private AutoResolver autoResolver;

    @BeforeEach
    void setUp() {
        autoResolver = new AutoResolver(validationRunner);
    }

    @Test
    void shouldResolve_WhenCorrectionsFixErrorsOnFirstIteration() {
        Map<String, FeatureFlag> state = new HashMap<>();
        FeatureFlag parent = FeatureFlag.builder().name("Parent").active(false).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(true).build();
        state.put("Parent", parent);
        state.put("Child", child);

        ValidationError err = ValidationError.builder()
                .type(ErrorType.HIERARCHY)
                .sourceFlag("Child")
                .targetFlag("Parent")
                .build();
        List<ValidationError> initialErrors = Collections.singletonList(err);

        // First iteration of executeValidation inside resolve() returns empty (meaning fixed!)
        when(validationRunner.executeValidation(any())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> autoResolver.resolve(state, initialErrors));

        // It should have turned off the child
        assertFalse(state.get("Child").isActive());
        verify(validationRunner, times(1)).executeValidation(any());
    }

    @Test
    void shouldResolve_WhenCorrectionsTakeMultipleIterations() {
        Map<String, FeatureFlag> state = new HashMap<>();
        FeatureFlag root = FeatureFlag.builder().name("Root").active(false).build();
        FeatureFlag child = FeatureFlag.builder().name("Child").active(true).build();
        FeatureFlag subChild = FeatureFlag.builder().name("SubChild").active(true).build();
        state.put("Root", root);
        state.put("Child", child);
        state.put("SubChild", subChild);

        ValidationError err1 = ValidationError.builder()
                .type(ErrorType.HIERARCHY)
                .sourceFlag("Child")
                .targetFlag("Root")
                .build();
                
        ValidationError err2 = ValidationError.builder()
                .type(ErrorType.HIERARCHY)
                .sourceFlag("SubChild")
                .targetFlag("Child")
                .build();

        // 1. Initial errors passed to resolve: err1
        // 2. Iteration 1 runs, fixes Child. validationRunner returns err2 (SubChild now orphan)
        // 3. Iteration 2 runs, fixes SubChild. validationRunner returns empty.
        when(validationRunner.executeValidation(any()))
                .thenReturn(Collections.singletonList(err2))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> autoResolver.resolve(state, Collections.singletonList(err1)));

        assertFalse(state.get("Child").isActive());
        assertFalse(state.get("SubChild").isActive());
        verify(validationRunner, times(2)).executeValidation(any());
    }

    @Test
    void shouldThrowException_WhenResolutionFailsToConvergeAfterMaxIterations() {
        Map<String, FeatureFlag> state = new HashMap<>();
        FeatureFlag flag = FeatureFlag.builder().name("F1").active(true).build();
        state.put("F1", flag);

        ValidationError err = ValidationError.builder()
                .type(ErrorType.SEMANTIC) // Semantic cannot be auto-resolved
                .sourceFlag("F1")
                .build();
        List<ValidationError> initialErrors = Collections.singletonList(err);

        // Always returns error (simulating deadlock)
        when(validationRunner.executeValidation(any())).thenReturn(Collections.singletonList(err));

        FeatureFlagValidationException exception = assertThrows(FeatureFlagValidationException.class,
                () -> autoResolver.resolve(state, initialErrors));

        assertTrue(exception.getMessage().contains("deadlock"));
        verify(validationRunner, times(4)).executeValidation(any());
    }
}
