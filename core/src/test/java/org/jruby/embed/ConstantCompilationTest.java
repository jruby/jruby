package org.jruby.embed;

import junit.framework.TestCase;

import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.runtime.Constants;

// JRUBY-5501: When embedding jruby the FORCE compile option breaks constants
public class ConstantCompilationTest extends TestCase{
    public void testConstantCompilation(){
        ScriptingContainer c = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
        //c.setCompileMode(CompileMode.FORCE);
        //EmbedEvalUnit unit = c.parse("RUBY_VERSION", 0);
        // FIXME: Do nothing test right now
        //assertEquals(Constants.RUBY_VERSION, unit.run().toString());
    }
}
