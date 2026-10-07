package com.cosmocats.marketplace.application.exception;

/** The delivery provider did not answer within the configured timeout. */
public class DeliveryTimeoutException extends DeliveryServiceException {

    public DeliveryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
