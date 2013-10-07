package org.jruby.embed;

import junit.framework.TestCase;

import org.jruby.RubyInstanceConfig.CompileMode;

// JRUBY-5501: When embedding jruby the FORCE compile option breaks constants
public class ConstantCompilationTest extends TestCase{
    public void testConstantCompilation(){
        ScriptingContainer c = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
        c.setCompileMode(CompileMode.FORCE);
        EmbedEvalUnit unit = c.parse("RUBY_VERSION", 0);
        assertEquals("2.0.0", unit.run().toString());
    }
}
