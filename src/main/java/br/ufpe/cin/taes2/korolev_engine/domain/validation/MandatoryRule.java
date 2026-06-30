package br.ufpe.cin.taes2.korolev_engine.domain.validation;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Rule B — Mandatory constraint.
 * If a child is marked as mandatory and its parent is active,
 * then the child must also be active.
 */
@Component
@Order(2)
public class MandatoryRule implements ValidationRule {

    @Override
    public void validate(FeatureFlag flag, Map<String, FeatureFlag> flagMap) {
        if (flag.getParentName() == null || !flag.isMandatory()) {
            return;
        }

        FeatureFlag parent = flagMap.get(flag.getParentName());

        if (parent != null && parent.isActive() && !flag.isActive()) {
            throw new FeatureFlagValidationException(
                    String.format("Erro de Mandatoriedade: A feature pai [%s] está ativa, então a feature filha mandatória [%s] deve estar ativa.",
                            flag.getParentName(), flag.getName())
            );
        }
    }
}
