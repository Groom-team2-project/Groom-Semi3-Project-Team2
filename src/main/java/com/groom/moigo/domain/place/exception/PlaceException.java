package com.groom.moigo.domain.place.exception;

import lombok.Getter;

@Getter
public class PlaceException extends RuntimeException {
    private final transient PlaceErrorCode errorCode;

    public PlaceException(PlaceErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PlaceException(PlaceErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
