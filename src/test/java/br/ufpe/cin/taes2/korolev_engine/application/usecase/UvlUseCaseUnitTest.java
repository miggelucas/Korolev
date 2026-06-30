package br.ufpe.cin.taes2.korolev_engine.application.usecase;

import br.ufpe.cin.taes2.korolev_engine.domain.service.UvlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UvlUseCaseUnitTest {

    @Mock
    private UvlService uvlService;

    @InjectMocks
    private UvlUseCase useCase;

    @Test
    void shouldCallUvlService_WhenImportingUvl() {
        String uvl = "features\n";
        useCase.importUvl(uvl);
        verify(uvlService, times(1)).importUvl(uvl);
    }

    @Test
    void shouldCallUvlService_WhenExportingUvl() {
        String uvl = "features\n";
        when(uvlService.exportUvl()).thenReturn(uvl);

        String result = useCase.exportUvl();

        assertEquals(uvl, result);
        verify(uvlService, times(1)).exportUvl();
    }
}
