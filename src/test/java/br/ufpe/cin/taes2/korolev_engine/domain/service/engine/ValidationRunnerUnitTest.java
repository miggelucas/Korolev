package br.ufpe.cin.taes2.korolev_engine.domain.service.engine;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ErrorType;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationRunnerUnitTest {

    @Mock
    private ValidationRule<FeatureFlag> rule1;

    @Mock
    private ValidationRule<FeatureFlag> rule2;

    private ValidationRunner validationRunner;

    @BeforeEach
    void setUp() {
        validationRunner = new ValidationRunner(Arrays.asList(rule1, rule2));
    }

    @Test
    void shouldReturnEmptyList_WhenNoErrorsAreFound() {
        FeatureFlag flag1 = FeatureFlag.builder().name("F1").build();
        FeatureFlag flag2 = FeatureFlag.builder().name("F2").build();
        List<FeatureFlag> flags = Arrays.asList(flag1, flag2);

        when(rule1.validate(any(), any())).thenReturn(Collections.emptyList());
        when(rule2.validate(any(), any())).thenReturn(Collections.emptyList());

        List<ValidationError> errors = validationRunner.executeValidation(flags);

        assertTrue(errors.isEmpty());
        verify(rule1, times(2)).validate(any(), any());
        verify(rule2, times(2)).validate(any(), any());
    }

    @Test
    void shouldAggregateErrors_WhenMultipleRulesFail() {
        FeatureFlag flag = FeatureFlag.builder().name("F1").build();
        List<FeatureFlag> flags = Collections.singletonList(flag);

        ValidationError error1 = ValidationError.builder().type(ErrorType.HIERARCHY).build();
        ValidationError error2 = ValidationError.builder().type(ErrorType.MANDATORY).build();

        when(rule1.validate(eq(flag), any())).thenReturn(Collections.singletonList(error1));
        when(rule2.validate(eq(flag), any())).thenReturn(Collections.singletonList(error2));

        List<ValidationError> errors = validationRunner.executeValidation(flags);

        assertEquals(2, errors.size());
        assertTrue(errors.contains(error1));
        assertTrue(errors.contains(error2));
    }

    @Test
    void shouldThrowException_WhenValidateGlobalStateFails() {
        FeatureFlag flag = FeatureFlag.builder().name("F1").build();
        ValidationError error = ValidationError.builder().type(ErrorType.SEMANTIC).build();

        when(rule1.validate(eq(flag), any())).thenReturn(Collections.singletonList(error));

        FeatureFlagValidationException exception = assertThrows(FeatureFlagValidationException.class,
                () -> validationRunner.validateGlobalState(Collections.singletonList(flag)));

        assertEquals(1, exception.getErrors().size());
        assertEquals(error, exception.getErrors().get(0));
    }

    @Test
    void shouldNotThrowException_WhenValidateGlobalStatePasses() {
        FeatureFlag flag = FeatureFlag.builder().name("F1").build();

        when(rule1.validate(any(), any())).thenReturn(Collections.emptyList());
        when(rule2.validate(any(), any())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> validationRunner.validateGlobalState(Collections.singletonList(flag)));
    }
}
