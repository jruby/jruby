package org.jruby;

import java.io.IOException;
import java.util.List;

import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ObjectMarshal;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.component.VariableEntry;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

public class RubySystemCallError extends RubyException {
    private IRubyObject errno;

    protected RubySystemCallError(Ruby runtime, RubyClass rubyClass) {
        this(runtime, rubyClass, null, 0);
    }

    public RubySystemCallError(Ruby runtime, RubyClass rubyClass, String message, int errno) {
        super(runtime, rubyClass, message);
        
        this.errno = runtime.newFixnum(errno);
    }

    private static ObjectAllocator SYSTEM_CALL_ERROR_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyException instance = new RubySystemCallError(runtime, klass);
            
            instance.setMetaClass(klass);
            
            return instance;
        }
    };
    
    private static final ObjectMarshal SYSTEM_CALL_ERROR_MARSHAL = new ObjectMarshal() {
        public void marshalTo(Ruby runtime, Object obj, RubyClass type,
                              MarshalStream marshalStream) throws IOException {
            RubySystemCallError exc = (RubySystemCallError) obj;
            
            List<Variable<IRubyObject>> attrs = exc.getVariableList();
            attrs.add(new VariableEntry<IRubyObject>(
                    "mesg", exc.message == null ? runtime.getNil() : exc.message));
            attrs.add(new VariableEntry<IRubyObject>("errno", exc.errno));
            attrs.add(new VariableEntry<IRubyObject>("bt", exc.getBacktrace()));
            marshalStream.dumpVariables(attrs);
        }

        public Object unmarshalFrom(Ruby runtime, RubyClass type,
            UnmarshalStream unmarshalStream) throws IOException {
            RubySystemCallError exc = (RubySystemCallError) type.allocate();
            
            unmarshalStream.registerLinkTarget(exc);
            unmarshalStream.defaultVariablesUnmarshal(exc);
            
            exc.message = exc.removeInternalVariable("mesg");
            exc.errno = exc.removeInternalVariable("errno");
            exc.set_backtrace(exc.removeInternalVariable("bt"));
            
            return exc;
        }
    };

    public static RubyClass createSystemCallErrorClass(Ruby runtime, RubyClass standardError) {
        RubyClass exceptionClass = runtime.defineClass("SystemCallError", standardError, SYSTEM_CALL_ERROR_ALLOCATOR);

        exceptionClass.setMarshal(SYSTEM_CALL_ERROR_MARSHAL);
        
        runtime.callbackFactory(RubyClass.class);
        exceptionClass.defineAnnotatedMethods(RubySystemCallError.class);

        return exceptionClass;
    }
    
    @JRubyMethod(optional = 2, required=1, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        if (args.length >= 1) message = args[0];
        if (args.length == 2) {
            errno = args[1].convertToInteger();
        } else {
            // FIXME: Get current errno value if one is set?
           errno = getRuntime().getNil();
        }
        
        return this;
    }

    @JRubyMethod
    public IRubyObject errno() {
        return errno;
    }
}
