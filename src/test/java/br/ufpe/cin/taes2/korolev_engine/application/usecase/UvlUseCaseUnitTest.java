package br.ufpe.cin.taes2.korolev_engine.application.usecase;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.service.UvlService;
import br.ufpe.cin.taes2.korolev_engine.domain.service.engine.KorolevEngine;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.repository.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UvlUseCaseUnitTest {

    @Mock
    private UvlService uvlService;

    @Mock
    private KorolevEngine korolevEngine;

    @Mock
    private FeatureFlagRepository repository;

    @InjectMocks
    private UvlUseCase useCase;

    @Test
    void shouldImportUvlAndPersist_WhenValid() {
        String uvl = "features\n";
        FeatureFlag flag = FeatureFlag.builder().name("F1").build();
        List<FeatureFlag> parsedFlags = Collections.singletonList(flag);

        when(uvlService.parseUvl(uvl)).thenReturn(parsedFlags);

        useCase.importUvl(uvl);

        verify(uvlService, times(1)).parseUvl(uvl);
        verify(korolevEngine, times(1)).validateGlobalState(parsedFlags);
        verify(repository, times(1)).clear();
        verify(repository, times(1)).save(flag);
    }

    @Test
    void shouldExportUvlFromRepositoryState() {
        String expectedUvl = "features\nF1";
        FeatureFlag flag = FeatureFlag.builder().name("F1").build();
        List<FeatureFlag> repoFlags = Collections.singletonList(flag);

        when(repository.findAll()).thenReturn(repoFlags);
        when(uvlService.exportUvl(repoFlags)).thenReturn(expectedUvl);

        String result = useCase.exportUvl();

        assertEquals(expectedUvl, result);
        verify(repository, times(1)).findAll();
        verify(uvlService, times(1)).exportUvl(repoFlags);
    }
}
