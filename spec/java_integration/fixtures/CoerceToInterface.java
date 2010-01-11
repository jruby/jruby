package java_integration.fixtures;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.JavaEmbedUtils;

public class CoerceToInterface {
    public Object returnArgumentBackToRuby(Ruby runtime, Runnable runnable) {
        return runnable;
    }

    public IRubyObject coerceArgumentBackToRuby(Ruby runtime, Runnable runnable) {
        return JavaEmbedUtils.javaToRuby(runtime, runnable);
    }

    public void passArgumentToInvokableRubyObject(IRubyObject recv, Runnable runnable) {
        recv.callMethod(recv.getRuntime().getCurrentContext(), "call", coerceArgumentBackToRuby(recv.getRuntime(), runnable));
    }
}
