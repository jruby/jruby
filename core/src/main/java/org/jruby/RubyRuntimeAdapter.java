
package org.jruby;

import java.io.InputStream;
import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;
import org.jruby.runtime.builtin.IRubyObject;

public interface RubyRuntimeAdapter {
    IRubyObject eval(Ruby runtime, String script);
    EvalUnit parse(Ruby runtime, String script, String filename, int lineNumber);
    EvalUnit parse(Ruby runtime, InputStream in, String filename, int lineNumber);
}
