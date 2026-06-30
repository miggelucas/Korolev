package br.ufpe.cin.taes2.korolev_engine.domain.exception;

/**
 * Thrown when trying to access or update a feature flag that does not exist.
 */
public class FeatureFlagNotFoundException extends FeatureFlagException {
    public FeatureFlagNotFoundException(String message) {
        super(message);
    }
}
