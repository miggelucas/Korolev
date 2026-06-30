package br.ufpe.cin.taes2.korolev_engine.application.usecase;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.service.GraphService;
import br.ufpe.cin.taes2.korolev_engine.domain.service.KorolevEngine;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.repository.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagUseCaseUnitTest {

    @Mock
    private KorolevEngine korolevEngine;

    @Mock
    private FeatureFlagRepository repository;

    @Mock
    private GraphService graphService;

    @InjectMocks
    private FeatureFlagUseCase useCase;

    @Test
    void shouldLoadState_ValidateCreation_ThenPersist() {
        FeatureFlag flag = FeatureFlag.builder().name("Test").active(true).build();
        List<FeatureFlag> currentState = Collections.emptyList();

        when(repository.findAll()).thenReturn(currentState);

        useCase.createFlag(flag);

        verify(repository, times(1)).findAll();
        verify(korolevEngine, times(1)).validateCreation(flag, currentState);
        verify(repository, times(1)).save(flag);
    }

    @Test
    void shouldLoadState_ValidateStateUpdate_ThenPersistReturnedFlags() {
        Map<String, Boolean> states = Map.of("F1", true);
        List<FeatureFlag> currentState = List.of(
                FeatureFlag.builder().name("F1").active(false).build()
        );
        FeatureFlag updatedFlag = FeatureFlag.builder().name("F1").active(true).build();

        when(repository.findAll()).thenReturn(currentState);
        when(korolevEngine.validateStateUpdate(states, currentState)).thenReturn(List.of(updatedFlag));

        useCase.updateFlagStates(states);

        verify(repository, times(1)).findAll();
        verify(korolevEngine, times(1)).validateStateUpdate(states, currentState);
        verify(repository, times(1)).save(updatedFlag);
    }

    @Test
    void shouldLoadState_ValidateDeletion_ThenDelete() {
        String name = "Test";
        List<FeatureFlag> currentState = List.of(
                FeatureFlag.builder().name(name).active(true).build()
        );

        when(repository.findAll()).thenReturn(currentState);

        useCase.deleteFlag(name);

        verify(repository, times(1)).findAll();
        verify(korolevEngine, times(1)).validateDeletion(name, currentState);
        verify(repository, times(1)).deleteByName(name);
    }

    @Test
    void shouldCallRepositoryFindAll_WhenGettingAllFlags() {
        List<FeatureFlag> expectedFlags = List.of(
                FeatureFlag.builder().name("Test").active(true).build()
        );
        when(repository.findAll()).thenReturn(expectedFlags);

        List<FeatureFlag> result = useCase.getAllFlags();

        assertEquals(expectedFlags, result);
        verify(repository, times(1)).findAll();
    }

    @Test
    void shouldCallRepositoryFindByName_WhenGettingFlagByName() {
        String name = "Test";
        FeatureFlag expectedFlag = FeatureFlag.builder().name(name).active(true).build();
        when(repository.findByName(name)).thenReturn(Optional.of(expectedFlag));

        Optional<FeatureFlag> result = useCase.getFlagByName(name);

        assertTrue(result.isPresent());
        assertEquals(expectedFlag, result.get());
    }

    @Test
    void shouldCallGraphService_WhenGettingGraphRepresentation() {
        String expectedGraph = "Graph Representation";
        when(graphService.renderGraph()).thenReturn(expectedGraph);

        String result = useCase.getGraphRepresentation();

        assertEquals(expectedGraph, result);
    }
}
