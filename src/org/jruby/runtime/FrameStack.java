package org.jruby.runtime;

import org.jruby.internal.util.collections.Stack;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class FrameStack extends Stack {
    private final ThreadContext threadContext;

    public FrameStack(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    public Frame getPrevious() {
        if (isEmpty()) {
        	return null;	
        }
        return (Frame) previous();
    }

    public void push() {
        push(new Frame(null, null, null, null, threadContext.getPosition(), threadContext.getCurrentIter()));
    }

    public void pushCopy() {
        push(((Frame) peek()).duplicate());
    }

    /**
     * @see Stack#pop()
     */
    public Object pop() {
        final Frame frame  = (Frame) super.pop();
        threadContext.setPosition(frame.getPosition());
        return frame;
    }
}
