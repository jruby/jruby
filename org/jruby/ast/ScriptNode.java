package org.jruby.ast;

import org.ablaf.ast.*;
import org.jruby.ast.visitor.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ScriptNode extends AbstractNode {
    private INode beginNode;
    private INode bodyNode;
    
    public ScriptNode(INode beginNode, INode bodyNode) {
        super(null);
        
        this.beginNode = beginNode;
        this.bodyNode = bodyNode;
    }

    /**
     * @see AbstractNode#accept(NodeVisitor)
     */
    public void accept(NodeVisitor visitor) {
    }

    /**
     * Gets the beginNode.
     * @return Returns a INode
     */
    public INode getBeginNode() {
        return beginNode;
    }

    /**
     * Sets the beginNode.
     * @param beginNode The beginNode to set
     */
    public void setBeginNode(INode beginNode) {
        this.beginNode = beginNode;
    }

    /**
     * Gets the bodyNode.
     * @return Returns a INode
     */
    public INode getBodyNode() {
        return bodyNode;
    }

    /**
     * Sets the bodyNode.
     * @param bodyNode The bodyNode to set
     */
    public void setBodyNode(INode bodyNode) {
        this.bodyNode = bodyNode;
    }
}