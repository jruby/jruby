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
        return (Frame) top.next.data;
    }

    public void push() {
        Namespace ns = peek() != null ? ((Frame)peek()).getNamespace() : null;

        push(new Frame(null, null, null, null, ns, null, ruby.getSourceFile(), ruby.getSourceLine(), ruby.getActIter()));
    }

    /**
     * @see IStack#pop()
     */
    public Object pop() {
        Frame frame  = (Frame)super.pop();

        ruby.setSourceFile(frame.getFile());
        ruby.setSourceLine(frame.getLine());

        return frame;
    }
}