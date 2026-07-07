package br.ufpe.cin.taes2.korolev_engine.domain.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {
    private ErrorType type;
    private String message;
    private String sourceFlag;
    private String targetFlag;
    
    @Override
    public String toString() {
        return message;
    }
}
