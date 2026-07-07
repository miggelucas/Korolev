package br.ufpe.cin.taes2.korolev_engine.controller.dto;

import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String errorCode;
    private String message;
    private List<ValidationError> errors;
    private LocalDateTime timestamp;
}
