/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ext;

import java.lang.ref.WeakReference;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
@JRubyClass(name="WeakRef", parent="Delegator")
public class WeakRef extends RubyObject {
    private WeakReference<IRubyObject> ref;
    
    public static final ObjectAllocator WEAKREF_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new WeakRef(runtime, klazz);
        }
    };
    
    @JRubyClass(name="WeakRef::RefError", parent="StandardError")
    public static class RefError {}

    public WeakRef(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }
    
    @JRubyMethod(name = "__getobj__")
    public IRubyObject getobj() {
        IRubyObject obj = ref.get();
        
        if (obj == null) {
            // FIXME weakref.rb also does caller(2) here for the backtrace
            throw newRefError("Illegal Reference - probably recycled");
        }
        
        return obj;
    }

    @JRubyMethod(name = "__setobj__")
    public IRubyObject setobj(IRubyObject obj) {
        return getRuntime().getNil();
    }
    
    @JRubyMethod(name = "new", required = 1, meta = true)
    public static IRubyObject newInstance(IRubyObject clazz, IRubyObject arg) {
        WeakRef weakRef = (WeakRef)((RubyClass)clazz).allocate();
        
        weakRef.callInit(new IRubyObject[] {arg}, Block.NULL_BLOCK);
        
        return weakRef;
    }

    // framed for invokeSuper
    @JRubyMethod(frame = true, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject obj) {
        ref = new WeakReference<IRubyObject>(obj);
        
        return RuntimeHelpers.invokeSuper(context, this, obj, Block.NULL_BLOCK);
    }
    
    @JRubyMethod(name = "weakref_alive?")
    public IRubyObject weakref_alive_p() {
        return ref.get() != null ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    private RaiseException newRefError(String message) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        RubyException exception =
                (RubyException)runtime.getClass("WeakRef").getClass("RefError").newInstance(context,
                new IRubyObject[] {runtime.newString(message)}, Block.NULL_BLOCK);
        
        RaiseException re = new RaiseException(exception);
        return re;
    }
}
