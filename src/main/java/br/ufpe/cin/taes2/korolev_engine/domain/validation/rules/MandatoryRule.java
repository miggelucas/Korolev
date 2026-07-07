package br.ufpe.cin.taes2.korolev_engine.domain.validation.rules;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ErrorType;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Rule B — Mandatory constraint.
 * If a parent feature is active, its mandatory child features must also be active.
 */
@Component
@Order(2)
public class MandatoryRule implements ValidationRule<FeatureFlag> {

    @Override
    public ErrorType getErrorType() {
        return ErrorType.MANDATORY;
    }

    @Override
    public List<ValidationError> validate(FeatureFlag flag, Map<String, FeatureFlag> flagMap) {
        if (flag.getParentName() == null || !flag.isMandatory()) {
            return Collections.emptyList();
        }

        FeatureFlag parent = flagMap.get(flag.getParentName());

        if (parent != null && parent.isActive() && !flag.isActive()) {
            return List.of(buildError(
                    flag.getParentName(),
                    flag.getName(),
                    String.format("Erro de Mandatoriedade: A feature pai [%s] está ativa, então a feature filha mandatória [%s] deve estar ativa.",
                            flag.getParentName(), flag.getName())
            ));
        }
        
        return Collections.emptyList();
    }
}
