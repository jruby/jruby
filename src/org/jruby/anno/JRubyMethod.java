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

/**
 *
 * @author headius
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JRubyMethod {
    String name();
    int required() default 0;
    int optional() default 0;
    String alias() default "";
    boolean singleton() default false;
}
