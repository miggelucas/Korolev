package br.ufpe.cin.taes2.korolev_engine.domain.exception;

/**
 * Thrown when trying to create a feature flag that is already registered.
 */
public class FeatureFlagAlreadyExistsException extends FeatureFlagException {
    public FeatureFlagAlreadyExistsException(String message) {
        super(message);
    }
}
