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
    /**
     * The name or names of this method in Ruby-land.
     */
    String[] name() default {};
    /**
     * The number of required arguments.
     */
    int required() default 0;
    /**
     * The number of optional arguments.
     */
    int optional() default 0;
    /**
     * Whether this method has a "rest" argument.
     */
    boolean rest() default false;
    /**
     * Any alias or aliases for this method.
     */
    String[] alias() default {};
    /**
     * Whether this method should be defined on the metaclass.
     */
    boolean meta() default false;
    /**
     * Whether this method should be a module function, defined on metaclass and private on class.
     */
    boolean module() default false;
    /**
     * Whether this method expects to have a call frame allocated for it.
     */
    boolean frame() default false;
    /**
     * Whether this method expects to have a heap-based variable scope allocated for it.
     */
    boolean scope() default false;
    /**
     * Whether this method is specific to Ruby 1.9
     */
    boolean rite() default false;
    /**
     * The visibility of this method.
     */
    Visibility visibility() default Visibility.PUBLIC;
}
