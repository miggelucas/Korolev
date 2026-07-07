package br.ufpe.cin.taes2.korolev_engine.domain.validation.rules;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ErrorType;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationError;
import br.ufpe.cin.taes2.korolev_engine.domain.validation.ValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Rule C — Cross-Tree Requires constraint.
 * If feature A requires feature B, feature A can only be active if feature B is active.
 */
@Component
@Order(3)
public class CrossTreeRequiresRule implements ValidationRule<FeatureFlag> {

    @Override
    public ErrorType getErrorType() {
        return ErrorType.REQUIRES;
    }

    @Override
    public List<ValidationError> validate(FeatureFlag flag, Map<String, FeatureFlag> flagMap) {
        if (!flag.isActive() || flag.getRequiresList() == null || flag.getRequiresList().isEmpty()) {
            return Collections.emptyList();
        }

        List<ValidationError> errors = new ArrayList<>();
        for (String targetName : flag.getRequiresList()) {
            FeatureFlag target = flagMap.get(targetName);

            if (target == null || !target.isActive()) {
                errors.add(buildError(
                        flag.getName(),
                        targetName,
                        String.format("Erro de restrição cruzada (requires): A feature [%s] está ativa e requer a feature [%s], que está inativa ou não existe.",
                                flag.getName(), targetName)
                ));
            }
        }
        
        return errors;
    }
}
