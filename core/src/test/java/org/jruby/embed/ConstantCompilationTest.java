package org.jruby.embed;

import junit.framework.TestCase;

import org.jruby.CompatVersion;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

// JRUBY-5501: When embedding jruby the FORCE compile option breaks constants
public class ConstantCompilationTest extends TestCase{
    public void testConstantCompilation(){
        ScriptingContainer c = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
        c.setCompatVersion(CompatVersion.RUBY1_8);
        c.setCompileMode(CompileMode.FORCE);
        EmbedEvalUnit unit = c.parse("RUBY_VERSION", 0);
        assertEquals("1.8.7", unit.run().toString());
    }
}
