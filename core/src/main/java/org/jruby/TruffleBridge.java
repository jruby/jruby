package org.jruby;

import com.oracle.truffle.api.frame.MaterializedFrame;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.Node;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.TruffleMethod;
import org.jruby.truffle.translator.TranslatorDriver;

/**
 * Created by chrisseaton on 25/02/2014.
 */
public interface TruffleBridge {
    void init();

    TruffleMethod truffelize(DynamicMethod originalMethod, ArgsNode argsNode, Node bodyNode);

    Object execute(TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, org.jruby.ast.RootNode rootNode);

    IRubyObject toJRuby(Object object);

    Object toTruffle(IRubyObject object);

    void shutdown();
}
