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
@Target(ElementType.TYPE)
public @interface JRubyClass {
    String[] name();
    //This parameter should only be used to point out if something is a Module instead of a Class
    String parent() default "Object";
    String[] include() default {};
}
