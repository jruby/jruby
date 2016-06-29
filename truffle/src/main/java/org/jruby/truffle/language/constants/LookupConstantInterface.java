package org.jruby.truffle.language.constants;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyConstant;

public interface LookupConstantInterface {

    public abstract RubyConstant lookupConstant(VirtualFrame frame, Object module, String name);

}
