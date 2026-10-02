package com.fastvue.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/** BCrypt accepts at most 72 UTF-8 bytes, regardless of Java character count. */
@Target({
    ElementType.FIELD,
    ElementType.PARAMETER,
    ElementType.RECORD_COMPONENT,
    ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BcryptPasswordValidator.class)
public @interface BcryptPassword {
    String message() default "密码 UTF-8 编码不能超过 72 字节";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
