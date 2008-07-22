package org.jruby.javasupport.util;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaRubyConverter {
    public static Object convertProcToInterface(ThreadContext context, RubyObject rubyObject, Class target) {
        Ruby runtime = context.getRuntime();
        IRubyObject javaUtilities = runtime.getJavaSupport().getJavaUtilitiesModule();
        IRubyObject javaInterfaceModule = Java.get_interface_module(javaUtilities, JavaClass.get(runtime, target));
        if (!((RubyModule) javaInterfaceModule).isInstance(rubyObject)) {
            rubyObject.extend(new IRubyObject[]{javaInterfaceModule});
        }

        if (rubyObject instanceof RubyProc) {
            // Proc implementing an interface, pull in the catch-all code that lets the proc get invoked
            // no matter what method is called on the interface
            RubyClass singletonClass = rubyObject.getSingletonClass();
            final RubyProc proc = (RubyProc) rubyObject;

            singletonClass.addMethod("method_missing", new DynamicMethod(singletonClass, Visibility.PUBLIC, CallConfiguration.NO_FRAME_NO_SCOPE) {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    IRubyObject[] newArgs;
                    if (args.length == 1) {
                        newArgs = IRubyObject.NULL_ARRAY;
                    } else {
                        newArgs = new IRubyObject[args.length - 1];
                        System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                    }
                    return proc.call(context, newArgs);
                }

                @Override
                public DynamicMethod dup() {
                    return this;
                }
            });
        }
        JavaObject jo = (JavaObject) rubyObject.instance_eval(context, runtime.newString("send :__jcreate_meta!"), Block.NULL_BLOCK);
        return jo.getValue();
    }
}
