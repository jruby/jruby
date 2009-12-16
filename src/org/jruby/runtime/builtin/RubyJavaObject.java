/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.runtime.builtin;

import org.jruby.RubyClass;

/**
 *
 * @author headius
 */
public interface RubyJavaObject {
    public RubyClass getMetaClass();
}
