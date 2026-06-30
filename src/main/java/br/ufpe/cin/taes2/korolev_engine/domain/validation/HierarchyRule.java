package br.ufpe.cin.taes2.korolev_engine.domain.validation;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Rule A — Hierarchy constraint.
 * A child feature can only be active if its parent node is also active.
 */
@Component
@Order(1)
public class HierarchyRule implements ValidationRule {

    @Override
    public void validate(FeatureFlag flag, Map<String, FeatureFlag> flagMap) {
        if (!flag.isActive() || flag.getParentName() == null) {
            return;
        }

        FeatureFlag parent = flagMap.get(flag.getParentName());

        if (parent == null || !parent.isActive()) {
            throw new FeatureFlagValidationException(
                    String.format("Erro de Hierarquia: A feature [%s] está ativa, mas seu pai [%s] não está ativo.",
                            flag.getName(), flag.getParentName())
            );
        }
    }
}
