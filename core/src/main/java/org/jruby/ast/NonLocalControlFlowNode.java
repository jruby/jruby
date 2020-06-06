/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

/**
 * Marker interface for return, break, next, redo, retry
 */
public interface NonLocalControlFlowNode {
    public Node getValueNode();
    public boolean hasValue();
}
