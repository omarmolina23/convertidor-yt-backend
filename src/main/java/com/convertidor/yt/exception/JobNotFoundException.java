package com.convertidor.yt.exception;

/**
 * Se lanza cuando se consulta un trabajo inexistente o ya expirado.
 */
public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(String id) {
        super("No existe el trabajo con id: " + id);
    }
}
