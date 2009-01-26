/*
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
@Target(ElementType.PARAMETER)
public @interface Coercion {
    CoercionType value();
}
