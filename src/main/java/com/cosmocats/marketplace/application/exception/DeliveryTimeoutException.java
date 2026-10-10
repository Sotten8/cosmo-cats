package com.cosmocats.marketplace.application.exception;

public class DeliveryTimeoutException extends DeliveryServiceException {

    public DeliveryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
