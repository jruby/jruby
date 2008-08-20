/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.internal.runtime.methods;

import org.jruby.ast.ArgsNode;

/**
 * Any class which can expose information about the arguments it requires
 */
public interface MethodArgs {
    public ArgsNode getArgsNode();
}
