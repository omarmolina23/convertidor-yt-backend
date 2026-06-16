package com.convertidor.yt.exception;

/**
 * Error de negocio durante la descarga/conversión.
 */
public class ConversionException extends RuntimeException {

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
