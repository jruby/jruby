package org.jruby.ast;

import org.ablaf.common.*;
import org.jruby.ast.visitor.*;

/** Represents a zero arg in a block.
 * 
 * <pre>
 * do ||
 * end
 * </pre>
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ZeroArgNode extends AbstractNode {

    /**
     * Constructor for ZeroArgNode.
     * @param position
     */
    public ZeroArgNode() {
        super(null);
    }

    /**
     * @see AbstractNode#accept(NodeVisitor)
     */
    public void accept(NodeVisitor visitor) {
    }
}