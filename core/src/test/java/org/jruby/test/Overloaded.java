/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.test;

/**
 *
 * @author headius
 */
public interface Overloaded {
    public String getName(String name);
    public String getName(String name, Integer age);
    public Object foo(Object foo);
}
