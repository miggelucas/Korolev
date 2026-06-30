package br.ufpe.cin.taes2.korolev_engine.domain.exception;

/**
 * Base abstract exception for all Korolev Domain exceptions.
 */
public abstract class FeatureFlagException extends RuntimeException {
    public FeatureFlagException(String message) {
        super(message);
    }
}
