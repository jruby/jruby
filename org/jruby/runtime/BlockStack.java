package org.jruby.runtime;

import java.util.*;

import org.ablaf.ast.*;
import org.jruby.*;
import org.jruby.runtime.methods.*;
import org.jruby.util.collections.*;
import org.jruby.util.collections.StackElement;

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

    public void push(INode varNode, IMethod method, RubyObject self) {
        push(new Block(varNode, method, self, ruby.getActFrame(), (Scope)ruby.getScope().getTop(), ruby.getRubyClass(), ruby.getActIter(), ruby.getDynamicVars(), null));
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