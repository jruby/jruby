package org.jruby.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jruby.CompatVersion;
import org.jruby.runtime.Visibility;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JRubyConstant {
    String[] value() default {};
}
