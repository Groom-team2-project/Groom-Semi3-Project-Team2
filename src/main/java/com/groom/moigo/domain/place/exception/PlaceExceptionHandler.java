package com.groom.moigo.domain.place.exception;

import com.groom.moigo.domain.place.controller.PlaceController;
import com.groom.moigo.global.response.CommonResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PlaceController.class)
public class PlaceExceptionHandler {
    @ExceptionHandler(PlaceException.class)
    public ResponseEntity<CommonResponse<Void>> handlePlaceException(PlaceException exception) {
        PlaceErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(CommonResponse.error(errorCode.name(), exception.getMessage()));
    }
}
