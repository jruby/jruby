package org.jruby.ast;

import org.ablaf.ast.visitor.INodeVisitor;

/** Represents a zero arg in a block.
 * this is never visited and is used only in an instanceof check
 * <pre>
 * do ||
 * end
 * </pre>
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ZeroArgNode extends AbstractNode {
    static final long serialVersionUID = 6596791950608957025L;


    /**
     * Constructor for ZeroArgNode.
     * @param position
     */
    public ZeroArgNode() {
        super();
    }

    /**
     * @see AbstractNode#accept(NodeVisitor)
     */
    public void accept(INodeVisitor visitor) {
    }
}
