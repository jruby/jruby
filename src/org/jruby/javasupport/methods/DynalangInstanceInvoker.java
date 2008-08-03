package org.jruby.javasupport.methods;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.dynalang.mop.BaseMetaobjectProtocol.Results;
import org.dynalang.mop.beans.BeansMetaobjectProtocol;
import org.dynalang.mop.CallProtocol;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DynalangInstanceInvoker extends MethodInvoker {
    protected BeansMetaobjectProtocol protocol;

    public DynalangInstanceInvoker(RubyClass host, List<Method> methods) {
        super(host, methods);
        protocol = new BeansMetaobjectProtocol(true);
    }
    
    private JavaUtil.JavaConverter getReturnConverter(Class type) {
        return JavaUtil.getJavaConverter(type);
    }
    
    private IRubyObject coerceResult(ThreadContext context, Object result) {
        if (result == null) {
            return context.getRuntime().getNil();
        }
        IRubyObject rubyResult = getReturnConverter(result.getClass()).convert(context.getRuntime(), result);
        
        return JavaUtil.java_to_ruby(context.getRuntime(), rubyResult);
    }
    
    public static CallProtocol RUBY_ARG_COERCION = new CallProtocol() {
        public Object representAs(Object src, Class target) {
            IRubyObject rubyObject = (IRubyObject)src;
            ThreadContext context = rubyObject.getRuntime().getCurrentContext();
            Object converted = JavaUtil.convertArgumentToType(context, rubyObject, target);
            
            if (converted == src && converted.getClass() != target) {
                return Results.noRepresentation;
            }
            
            return converted;
        }

        public Object get(Object arg0, Object arg1) {
            return Results.noAuthority;
        }

        public Object call(Object arg0, CallProtocol arg1, Map arg2) {
            return Results.noAuthority;
        }

        public Object call(Object arg0, CallProtocol arg1, Object... arg2) {
            return Results.noAuthority;
        }
    };

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        // Object[] cast is needed here to pass args as non-vararg
        return coerceResult(context, protocol.call(((JavaObject)self.dataGetStruct()).getValue(), name, RUBY_ARG_COERCION, (Object[])args));
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        return coerceResult(context, protocol.call(((JavaObject)self.dataGetStruct()).getValue(), name, RUBY_ARG_COERCION));
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        return coerceResult(context, protocol.call(((JavaObject)self.dataGetStruct()).getValue(), name, RUBY_ARG_COERCION, arg0));
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        return coerceResult(context, protocol.call(((JavaObject)self.dataGetStruct()).getValue(), name, RUBY_ARG_COERCION, arg0, arg1));
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return coerceResult(context, protocol.call(((JavaObject)self.dataGetStruct()).getValue(), name, RUBY_ARG_COERCION, arg0, arg1, arg2));
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        createJavaMethods(self.getRuntime());
        if (block.isGiven()) {
            int len = args.length;
            IRubyObject[] intermediate = new IRubyObject[len + 1];
            System.arraycopy(args, 0, intermediate, 0, len);
            intermediate[len] = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            // Object[] cast is needed here to pass args as non-vararg
            return coerceResult(context, protocol.call(((JavaObject)self.dataGetStruct()).getValue(), name, RUBY_ARG_COERCION, (Object[])intermediate));
        } else {
            return call(context, self, clazz, name, args);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        createJavaMethods(self.getRuntime());
        if (block.isGiven()) {
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            return coerceResult(context, protocol.call(((JavaObject)self.dataGetStruct()).getValue(), name, RUBY_ARG_COERCION, proc));
        } else {
            return call(context, self, clazz, name);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        createJavaMethods(self.getRuntime());
        if (block.isGiven()) {
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            return coerceResult(context, protocol.call(((JavaObject)self.dataGetStruct()).getValue(), name, RUBY_ARG_COERCION, arg0, proc));
        } else {
            return call(context, self, clazz, name, arg0);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        createJavaMethods(self.getRuntime());
        if (block.isGiven()) {
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            return coerceResult(context, protocol.call(((JavaObject)self.dataGetStruct()).getValue(), name, RUBY_ARG_COERCION, arg0, arg1, proc));
        } else {
            return call(context, self, clazz, name, arg0, arg1);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        createJavaMethods(self.getRuntime());
        if (block.isGiven()) {
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            return coerceResult(context, protocol.call(((JavaObject)self.dataGetStruct()).getValue(), name, RUBY_ARG_COERCION, arg0, arg1, arg2, proc));
        } else {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }
    }
}
