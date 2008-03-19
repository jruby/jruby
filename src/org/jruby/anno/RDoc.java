package org.jruby.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author olabini
 */
/* Seems runtime retention is necessary, since APT can't extract annotation information from class files */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RDoc {
    String[] callSeq() default "";
    String doc() default "";
    String[] name() default "";
    //This parameter should only be used to point out if something is a Module instead of a Class
    String type() default "Class";
    String parent() default "Object";
}
