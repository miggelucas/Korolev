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
 * Rule A — Hierarchy constraint.
 * A child feature can only be active if its parent node is also active.
 */
@Component
@Order(1)
public class HierarchyRule implements ValidationRule<FeatureFlag> {

    @Override
    public ErrorType getErrorType() {
        return ErrorType.HIERARCHY;
    }

    @Override
    public List<ValidationError> validate(FeatureFlag flag, Map<String, FeatureFlag> flagMap) {
        if (!flag.isActive() || flag.getParentName() == null) {
            return Collections.emptyList();
        }

        FeatureFlag parent = flagMap.get(flag.getParentName());
        if (parent == null || !parent.isActive()) {
            return List.of(buildError(
                    flag.getName(),
                    flag.getParentName(),
                    String.format("Erro de Hierarquia: A feature [%s] não pode estar ativa porque seu pai [%s] está inativo ou não existe.",
                            flag.getName(), flag.getParentName())
            ));
        }
        
        return Collections.emptyList();
    }
}
