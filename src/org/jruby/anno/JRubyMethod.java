/*
 * JRubyMethod.java
 * 
 * Created on Aug 4, 2007, 3:07:36 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jruby.runtime.Visibility;

/**
 *
 * @author headius
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JRubyMethod {
    String name() default "";
    String name2() default "";
    String name3() default "";
    String name4() default "";
    String name5() default "";
    String name6() default "";
    int required() default 0;
    int optional() default 0;
    boolean rest() default false;
    String alias() default "";
    boolean meta() default false;
    boolean module() default false;
    boolean frame() default false;
    boolean scope() default false;
    Visibility visibility() default Visibility.PUBLIC;
}
