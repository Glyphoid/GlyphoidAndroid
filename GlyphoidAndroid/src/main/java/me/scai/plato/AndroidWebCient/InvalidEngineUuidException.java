package me.scai.plato.AndroidWebCient;

import me.scai.parsetree.HandwritingEngineException;

public class InvalidEngineUuidException extends HandwritingEngineException {
    public InvalidEngineUuidException(String message) {
        super(message);
    }
}
