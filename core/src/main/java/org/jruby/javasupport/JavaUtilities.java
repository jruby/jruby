package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.StringSupport;

import static org.jruby.anno.FrameField.*;

@JRubyModule(name = "JavaUtilities")
public class JavaUtilities {
    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject set_java_object(IRubyObject recv, IRubyObject self, IRubyObject java_object) {
        self.dataWrapStruct(java_object);
        return java_object;
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_interface_module(IRubyObject recv, IRubyObject arg0) {
        return Java.get_interface_module(recv.getRuntime(), arg0);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_package_module(IRubyObject recv, IRubyObject arg0) {
        return Java.get_package_module(recv, arg0);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_package_module_dot_format(IRubyObject recv, IRubyObject arg0) {
        return Java.get_package_module_dot_format(recv, arg0);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_proxy_class(IRubyObject recv, IRubyObject arg0) {
        return Java.get_proxy_class(recv, arg0);
    }

    @Deprecated // no longer used
    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject create_proxy_class(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return Java.create_proxy_class(recv, arg0, arg1, arg2);
    }

    @Deprecated // no longer used
    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_java_class(IRubyObject recv, IRubyObject arg0) {
        final Ruby runtime = recv.getRuntime();
        Class<?> javaClass = Java.getJavaClass(runtime, arg0.asJavaString());
        return Java.getInstance(runtime, javaClass);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_top_level_proxy_or_package(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return Java.get_top_level_proxy_or_package(context, recv, arg0);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_proxy_or_package_under_package(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return Java.get_proxy_or_package_under_package(context, recv, arg0, arg1);
    }

    @JRubyMethod(name = "valid_java_identifier?", meta = true)
    public static IRubyObject valid_java_identifier_p(ThreadContext context, IRubyObject recv, IRubyObject name) {
        final String javaName = name.convertToString().decodeString();
        return RubyBoolean.newBoolean(context, validJavaIdentifier(javaName));
    }

    public static boolean validJavaIdentifier(final String javaName) {
        for (String frag : StringSupport.split(javaName, '.')) {
            if (frag.length() == 0) return false;
            if (!Character.isJavaIdentifierStart(frag.codePointAt(0))) return false;
            for (int i = 1; i < frag.length(); i++) {
                if (!Character.isJavaIdentifierPart(frag.codePointAt(i))) return false;
            }
        }
        return true;
    }
    
    @Deprecated // no longer used
    @JRubyMethod(meta = true,
            reads = { LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE },
            writes = { LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE })
    public static IRubyObject extend_proxy(ThreadContext context, IRubyObject recv, IRubyObject name, Block block) {
        return Java.get_proxy_class(recv, name).module_eval(context, block);
    }

}
