package org.jruby.runtime;

import org.ablaf.ast.INode;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.util.collections.AbstractStack;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class BlockStack extends AbstractStack {
    private Ruby ruby;
    
    public BlockStack(Ruby ruby) {
        this.ruby = ruby;
    }

    public void push(INode varNode, ICallable method, RubyObject self) {
        push(new Block(varNode, method, self, ruby.getActFrame(), ruby.currentScope(), ruby.getRubyClass(), ruby.getActIter(), ruby.getDynamicVars(), null));
    }

    public Block getAct() {
        return (Block)getTop();
    }

	/**
	 * @fixme (maybe save old block)
	 **/
    public void setAct(Block actBlock) {
        top = actBlock;
    }
}