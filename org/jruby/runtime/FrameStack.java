package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.internal.util.collections.Stack;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class FrameStack extends Stack {
    private Ruby ruby;
    
    public FrameStack(Ruby ruby) {
        this.ruby = ruby;
    }

    public Frame getPrevious() {
        if (isEmpty()) {
        	return null;	
        }
        return (Frame) previous();
    }

    public void push() {
        Namespace ns = peek() != null ? ((Frame)peek()).getNamespace() : null;

        push(new Frame(null, null, null, null, ns, null, ruby.getPosition(), ruby.getCurrentIter()));
    }

    /**
     * @see IStack#pop()
     */
    public Object pop() {
        Frame frame  = (Frame) super.pop();
        ruby.setPosition(frame.getPosition());
        return frame;
    }
}