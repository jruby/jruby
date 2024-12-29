package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBoolean;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.StringSupport;

import static org.jruby.anno.FrameField.*;
import static org.jruby.api.Convert.asBoolean;

@JRubyModule(name = "JavaUtilities")
public class JavaUtilities {
    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject set_java_object(IRubyObject recv, IRubyObject self, IRubyObject java_object) {
        self.dataWrapStruct(java_object);
        return java_object;
    }

    @Deprecated(since = "10.0")
    public static IRubyObject get_interface_module(IRubyObject recv, IRubyObject arg0) {
        return get_interface_module(((RubyBasicObject) recv).getCurrentContext(), recv, arg0);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_interface_module(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return Java.get_interface_module(context, arg0);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject get_package_module(IRubyObject recv, IRubyObject arg0) {
        return get_package_module(((RubyBasicObject) recv).getCurrentContext(), recv, arg0);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_package_module(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return Java.get_package_module(context, recv, arg0);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject get_package_module_dot_format(IRubyObject recv, IRubyObject arg0) {
        return get_package_module_dot_format(((RubyBasicObject) recv).getCurrentContext(), recv, arg0);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_package_module_dot_format(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return Java.get_package_module_dot_format(context, recv, arg0);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject get_proxy_class(IRubyObject recv, IRubyObject arg0) {
        return get_proxy_class(((RubyBasicObject) recv).getCurrentContext(), recv, arg0);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_proxy_class(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return Java.get_proxy_class(context, recv, arg0);
    }

    @Deprecated(since = "9.4-") // no longer used
    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject create_proxy_class(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return Java.create_proxy_class(recv, arg0, arg1, arg2);
    }

    @Deprecated(since = "9.4-") // no longer used
    public static IRubyObject get_java_class(IRubyObject recv, IRubyObject arg0) {
        return get_java_class(((RubyBasicObject) recv).getCurrentContext(), recv, arg0);
    }

    @Deprecated(since = "10.0") // no longer used
    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject get_java_class(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        Class<?> javaClass = Java.getJavaClass(context, arg0.asJavaString());
        return Java.getInstance(context.runtime, javaClass);
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
        return asBoolean(context, validJavaIdentifier(name.convertToString().decodeString()));
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
        return Java.get_proxy_class(context, recv, name).module_eval(context, block);
    }

}
