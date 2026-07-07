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
 * Ensures a feature flag model makes semantic sense before accepting it.
 * E.g., A flag shouldn't explicitly require itself or its direct parent.
 */
@Component
@Order(0)
public class SemanticValidationRule implements ValidationRule<FeatureFlag> {

    @Override
    public ErrorType getErrorType() {
        return ErrorType.SEMANTIC;
    }

    @Override
    public List<ValidationError> validate(FeatureFlag flag, Map<String, FeatureFlag> flagMap) {
        if (flag.getRequiresList() == null || flag.getRequiresList().isEmpty()) {
            return Collections.emptyList();
        }

        List<ValidationError> errors = new ArrayList<>();
        for (String target : flag.getRequiresList()) {
            if (flag.getName().equals(target)) {
                errors.add(buildError(
                        flag.getName(),
                        target,
                        String.format("Erro Semântico: A feature [%s] não pode requerer a si mesma.", flag.getName())
                ));
            }

            if (flag.getParentName() != null && flag.getParentName().equals(target)) {
                errors.add(buildError(
                        flag.getName(),
                        target,
                        String.format("Erro Semântico: A feature [%s] não pode declarar explicitamente que requer seu pai [%s], pois essa dependência já é garantida pela hierarquia.",
                                flag.getName(), flag.getParentName())
                ));
            }
        }
        return errors;
    }
}
