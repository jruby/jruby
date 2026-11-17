
package org.jruby;

import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

public interface RubyObjectAdapter {

    boolean isKindOf(IRubyObject value, RubyModule rubyModule);

    IRubyObject setInstanceVariable(IRubyObject obj, String variableName, IRubyObject value);

    IRubyObject[] convertToJavaArray(IRubyObject array);

    RubyInteger convertToRubyInteger(IRubyObject obj);

    IRubyObject getInstanceVariable(IRubyObject obj, String variableName);

    RubyString convertToRubyString(IRubyObject obj);
    
    // These call* assume ThreadContext = receiver.getRuntime().getCurrentContext()
    IRubyObject callMethod(IRubyObject receiver, String methodName);

    IRubyObject callMethod(IRubyObject receiver, String methodName, IRubyObject singleArg);

    IRubyObject callMethod(IRubyObject receiver, String methodName, IRubyObject[] args);

    IRubyObject callMethod(IRubyObject receiver, String methodName, IRubyObject[] args, Block block);

    IRubyObject callSuper(IRubyObject receiver, IRubyObject[] args);

    IRubyObject callSuper(IRubyObject receiver, IRubyObject[] args, Block block);
}
