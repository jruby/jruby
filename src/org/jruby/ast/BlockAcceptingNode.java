/*
 * BlockAcceptingNode.java
 *
 * Created on December 24, 2006, 12:48 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.ast;

/**
 *
 * @author headius
 */
public interface BlockAcceptingNode {
    public IterNode getIterNode();
    
    public void setIterNode(IterNode iterNode);
}