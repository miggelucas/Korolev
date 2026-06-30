package br.ufpe.cin.taes2.korolev_engine.domain.validation;

import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Rule D — Mutual exclusion constraint.
 * Two features that declare each other in their excludesList
 * cannot be active at the same time.
 */
@Component
@Order(4)
public class MutualExclusionRule implements ValidationRule {

    @Override
    public void validate(FeatureFlag flag, Map<String, FeatureFlag> flagMap) {
        if (!flag.isActive() || flag.getExcludesList() == null) {
            return;
        }

        for (String excludedName : flag.getExcludesList()) {
            FeatureFlag excluded = flagMap.get(excludedName);

            if (excluded != null && excluded.isActive()) {
                throw new FeatureFlagValidationException(
                        String.format("Conflito de Exclusividade Mútua: As flags [%s] e [%s] não podem estar ativas simultaneamente.",
                                flag.getName(), excludedName)
                );
            }
        }
    }
}
