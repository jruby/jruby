package org.jruby.runtime;

import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.internal.util.collections.Stack;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class FrameStack extends Stack {
    private Ruby runtime;
    
    public FrameStack(Ruby ruby) {
        this.runtime = ruby;
    }

    public Frame getPrevious() {
        if (isEmpty()) {
        	return null;	
        }
        return (Frame) previous();
    }

    public void push() {
        Namespace ns = peek() != null ? ((Frame)peek()).getNamespace() : null;

        push(new Frame(null, null, null, null, ns, null, runtime.getPosition(), runtime.getCurrentIter()));
    }

    /**
     * @see Stack#pop()
     */
    public Object pop() {
        Frame frame  = (Frame) super.pop();
        runtime.setPosition(frame.getPosition());
        return frame;
    }

    public FrameStack duplicate() {
        // FIXME don't create to much ArrayLists
        FrameStack newStack = new FrameStack(runtime);
        synchronized (list) {
            newStack.list = new ArrayList(list.size());
            for (int i = 0, size = list.size(); i < size; i++) {
                newStack.list.add(((Frame) list.get(i)).duplicate());
            }
        }
        return newStack;
    }
}