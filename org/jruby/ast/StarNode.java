package org.jruby.ast;

import org.ablaf.common.*;
import org.jruby.ast.visitor.*;
import org.ablaf.ast.visitor.INodeVisitor;

/** Represents a star in a multiple assignent.
 * only used in an instanceof check, this node is never visited.
 * @author  jpetersen
 * @version $Revision$
 */
public class StarNode extends AbstractNode {

    /**
     * Constructor for StartNode.
     * @param position
     */
    public StarNode() {
        super();
    }

    /**
     * @see AbstractNode#accept(NodeVisitor)
     */
    public void accept(INodeVisitor visitor) {
    }
}
