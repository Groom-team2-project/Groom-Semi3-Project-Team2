package com.groom.moigo.domain.place.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {
    private ValidEnum annotation;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext constraintValidatorContext
    ) {
        if (value == null) {
            return true;
        }

        Enum<?>[] enumValues = annotation.target().getEnumConstants();

        if (enumValues == null) {
            return false;
        }

        for (Enum<?> enumValue : enumValues) {
            if (enumValue.name().equals(value)) {
                return true;
            }
        }

        return false;
    }
}
