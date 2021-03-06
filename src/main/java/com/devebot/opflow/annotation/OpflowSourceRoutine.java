package com.devebot.opflow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author drupalex
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OpflowSourceRoutine {
    String alias() default "";
    boolean isAsync() default false;
    boolean skipped() default false;
}
