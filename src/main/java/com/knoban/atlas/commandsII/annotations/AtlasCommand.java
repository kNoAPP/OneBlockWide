package com.knoban.atlas.commandsII.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Alden Bansemer (kNoAPP)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AtlasCommand {

    String[] paths() default {};
    String description() default "";
    String permission() default "";
    String usage() default "";

    /* Higher number = higher priority. Defines which methods get registered first in a class
     * NOTE: This only guarantees priority of methods in the same class registered together.
     *
     * To be in 100% control of command priority between classes, register your methods in order of priority.
     */
    int classPriority() default 0;
}

