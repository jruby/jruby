package org.jruby.ast;

import org.ablaf.common.*;
import org.jruby.ast.visitor.*;

/** Represents a star in a multiple assignent.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class StarNode extends AbstractNode {

    /**
     * Constructor for StartNode.
     * @param position
     */
    public StarNode() {
        super(null);
    }

    /**
     * @see AbstractNode#accept(NodeVisitor)
     */
    public void accept(NodeVisitor visitor) {
    }
}