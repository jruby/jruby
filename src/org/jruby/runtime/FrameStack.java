package org.jruby.runtime;

import java.util.Stack;

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

    public synchronized Frame getPrevious() {
    	int size = size();
                
    	return size <= 1 ? null : (Frame) elementAt(size - 2);
    }

    public void push() {
        push(new Frame(null, null, null, null, threadContext.getPosition(), threadContext.getCurrentIter()));
    }

    public void pushCopy() {
        push(((Frame) peek()).duplicate());
    }

    public Object pop() {
        Frame frame = (Frame) super.pop();
        threadContext.setPosition(frame.getPosition());
        return frame;
    }
}
