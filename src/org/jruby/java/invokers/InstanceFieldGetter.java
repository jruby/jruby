package org.jruby.java.invokers;

import java.lang.reflect.Field;
import org.jruby.RubyModule;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class InstanceFieldGetter extends FieldMethodZero {

    public InstanceFieldGetter(String name, RubyModule host, Field field) {
        super(name, host, field);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        try {
            JavaProxy proxy = InstanceMethodInvoker.castJavaProxy(self);
            return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), field.get(proxy.getObject()));
        } catch (IllegalAccessException iae) {
            throw context.getRuntime().newTypeError("illegal access getting variable: " + iae.getMessage());
        }
    }
}
