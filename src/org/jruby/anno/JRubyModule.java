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
public @interface JRubyModule {
    String[] name();
    String[] include() default {};
}
