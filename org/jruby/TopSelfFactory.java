package org.jruby;

import org.jruby.runtime.Callback;
import org.jruby.runtime.IndexCallable;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public final class TopSelfFactory {

    /**
     * Constructor for TopSelfFactory.
     */
    private TopSelfFactory() {
        super();
    }
    
    public static IRubyObject createTopSelf(final Ruby runtime) {
        IRubyObject topSelf = runtime.getFactory().newObject(runtime.getClasses().getObjectClass());
        
        topSelf.defineSingletonMethod("to_s", new Callback() {
            /**
             * @see org.jruby.runtime.Callback#execute(IRubyObject, IRubyObject[])
             */
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
                return RubyString.newString(runtime, "main");
            }

            /**
             * @see org.jruby.runtime.Callback#getArity()
             */
            public int getArity() {
                return 0;
            }
        });
        
        topSelf.defineSingletonMethod("include", new Callback() {
            /**
             * @see org.jruby.runtime.Callback#execute(IRubyObject, IRubyObject[])
             */
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
                runtime.secure(4);
                return runtime.getClasses().getObjectClass().include(args);
            }

            /**
             * @see org.jruby.runtime.Callback#getArity()
             */
            public int getArity() {
                return 0;
            }
        });
        
        topSelf.defineSingletonMethod("public", new Callback() {
            /**
             * @see org.jruby.runtime.Callback#execute(IRubyObject, IRubyObject[])
             */
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
                return runtime.getClasses().getObjectClass().rbPublic(args);
            }

            /**
             * @see org.jruby.runtime.Callback#getArity()
             */
            public int getArity() {
                return 0;
            }
        });
        
        topSelf.defineSingletonMethod("private", new Callback() {
            /**
             * @see org.jruby.runtime.Callback#execute(IRubyObject, IRubyObject[])
             */
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
                return runtime.getClasses().getObjectClass().rbPrivate(args);
            }

            /**
             * @see org.jruby.runtime.Callback#getArity()
             */
            public int getArity() {
                return 0;
            }
        });
        
        return topSelf;
    }
}