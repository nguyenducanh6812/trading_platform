package com.ahd.trading_platform.marketdata.infrastructure.external;

/**
 * Exception thrown when external data client operations fail.
 * Used to wrap and provide context for external API failures.
 */
public class ExternalDataClientException extends RuntimeException {
    
    public ExternalDataClientException(String message) {
        super(message);
    }
    
    public ExternalDataClientException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ExternalDataClientException(Throwable cause) {
        super(cause);
    }
}