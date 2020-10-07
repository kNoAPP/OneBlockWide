package com.knoban.atlas.commandsII.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Alden Bansemer (kNoAPP)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AtlasParam {

    String filter() default "";
    String[] suggestions() default {};

}

