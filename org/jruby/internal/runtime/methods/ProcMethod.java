package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class ProcMethod extends AbstractMethod {
    private RubyProc proc;

    /**
     * Constructor for ProcMethod.
     * @param visibility
     */
    public ProcMethod(RubyProc proc, Visibility visibility) {
        super(visibility);
        this.proc = proc;
    }

    /**
     * @see org.jruby.runtime.ICallable#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        IRubyObject self = receiver;
        return proc.call(args, self);
    }
}