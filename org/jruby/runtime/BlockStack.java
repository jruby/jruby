package org.jruby.runtime;

import org.ablaf.ast.INode;
import org.jruby.RubyObject;
import org.jruby.util.collections.AbstractStack;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class BlockStack extends AbstractStack {

    public BlockStack() {
    }

    public void push(INode varNode, ICallable method, RubyObject self) {
        push(Block.createBlock(varNode, method, self));
    }

    public Block getCurrent() {
        return (Block) getTop();
    }

    public void setCurrent(Block block) {
        top = block;
    }
}