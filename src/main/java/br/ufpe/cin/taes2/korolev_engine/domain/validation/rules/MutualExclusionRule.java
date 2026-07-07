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
 * Rule D — Mutual Exclusion (Excludes) constraint.
 * If feature A excludes feature B, they cannot both be active at the same time.
 */
@Component
@Order(4)
public class MutualExclusionRule implements ValidationRule<FeatureFlag> {

    @Override
    public ErrorType getErrorType() {
        return ErrorType.EXCLUDES;
    }

    @Override
    public List<ValidationError> validate(FeatureFlag flag, Map<String, FeatureFlag> flagMap) {
        if (!flag.isActive() || flag.getExcludesList() == null || flag.getExcludesList().isEmpty()) {
            return Collections.emptyList();
        }

        List<ValidationError> errors = new ArrayList<>();
        for (String excludedName : flag.getExcludesList()) {
            FeatureFlag excluded = flagMap.get(excludedName);

            if (excluded != null && excluded.isActive()) {
                errors.add(buildError(
                        flag.getName(),
                        excludedName,
                        String.format("Erro de Exclusividade Mútua: As features [%s] e [%s] não podem estar ativas simultaneamente.",
                                flag.getName(), excludedName)
                ));
            }
        }
        return errors;
    }
}
