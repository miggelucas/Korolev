package br.ufpe.cin.taes2.korolev_engine.domain.validation;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Rule C — Cross-tree requires constraint.
 * An active feature that declares a requiresTarget dependency
 * can only be active if the target feature is also active.
 */
@Component
@Order(3)
public class CrossTreeRequiresRule implements ValidationRule {

    @Override
    public void validate(FeatureFlag flag, Map<String, FeatureFlag> flagMap) {
        if (!flag.isActive() || flag.getRequiresTarget() == null) {
            return;
        }

        String targetName = flag.getRequiresTarget();
        FeatureFlag target = flagMap.get(targetName);

        if (target == null || !target.isActive()) {
            throw new FeatureFlagValidationException(
                    String.format("Erro na restrição cruzada de [%s]: Requer que [%s] esteja ATIVA.",
                            flag.getName(), targetName)
            );
        }
    }
}
