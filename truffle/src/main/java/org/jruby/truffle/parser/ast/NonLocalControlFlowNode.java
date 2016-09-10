/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.truffle.parser.ast;

/**
 * Marker interface for return, break, next, redo, retry
 */
public interface NonLocalControlFlowNode {
    public ParseNode getValueNode();
    public boolean hasValue();
}
